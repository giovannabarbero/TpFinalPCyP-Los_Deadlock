// Guardamos cuantos tokens hay en cada plaza y si un hilo pide disparar, revisa si se puede
// Y se asegura de que dos hilos nunca toquen los tokens al mismo tiempo

package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import ar.edu.fcefyn.pcp.tp2.api.TransitionLogger;

import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CountDownLatch;

public class Monitor implements MonitorInterface {

    //atributos
    //int[][] es una matriz
    private final int[][] pre;          // I-: cuantos tokens consume cada transicion de cada plaza
    private final int[][] incidencia;   // I = I+ - I- cuanto cambia cada plaza cuando se dispara una transicion
    private final int[] marcado;        // tokens actuales en cada plaza
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition[] colas;   // una sala de espera por transición
    private final int[] esperando;     // cuantos hilos hay en cada sala
    private final int[][] invariantesP;   // una fila por invariante, y una columna por plaza osea es una matriz
    private final int[] sumInvariantesP;  // cuanto tiene que dar cada suma
    private final Politica politica;          // decide en los conflictos (se usa en el paso 4)
    private final TransitionLogger logger;  // registra cada disparo en el archivo de log
    private final CountDownLatch fin;    // avisa al Main que se completaron los invariantes
    private final int transicionFin;       // que transicion marca un invariante completo (T9)
    private final int objetivo;          // cuantas veces tiene que dispararse (200)
    private int disparosFin = 0;           // cuantas veces se disparo hasta ahora
    private boolean finished = false;       // true = el sistema termino
    private final long[] tiempoMS;              // tiempo minimo de cada transicion 0 = inmediata
    private final long[] sensibilizadaDesde;    // momento en que cada transicion quedo sensibilizada (-1 = no lo esta)
    private final int[] grupoDe;                                // a que grupo de conflicto pertenece cada transicion (-1 = ninguno)
    private final List<List<Integer>> grupos = new ArrayList<>(); // cada grupo es una lista de transiciones en conflicto
    private final int[] elegida;    // que transicion eligio la politica en cada grupo (-1 = todavia no eligio)
    private final boolean[] esInicio;   // transiciones que meten una transaccion nueva (T0)
    private int disparosInicio = 0;     // cuantas transacciones entraron

    //constructor
    //el monitor es agnostico solo recibe numeros si cambiamos la red cambiaria la configuracion y el Monitor queda igual
    public Monitor(PetriNetConfig config, Politica politica, TransitionLogger logger, CountDownLatch fin) {
        this.pre = config.matrixPre();
        this.incidencia = config.matrixIncidencia();
        this.marcado = config.marcadoInicial().clone();
        int n = config.numTransiciones();
        this.colas = new Condition[n];
        for (int i = 0; i < n; i++) {
            colas[i] = lock.newCondition();   // cada sala esta atada a este lock
        }
        this.esperando = new int[n];
        // los invariantes tambien vienen del config el monitor no sabe cuales son
        this.invariantesP = config.invariantesP();
        this.sumInvariantesP = config.sumInvariantesP();

        this.politica = politica;
        this.logger = logger;
        this.fin = fin;

        // la condicion de fin viene de config
        this.transicionFin = config.transicionFinInvariantes();
        this.objetivo = config.objetivoInvariantes();

        // detectamos la transicion de entrada: la que saca tokens de la plaza
// donde la transicion de fin (T9) devuelve el token (P0)
        this.esInicio = new boolean[n];
        for (int p = 0; p < pre.length; p++) {
            if (incidencia[p][transicionFin] > 0) {     // plaza donde T9 deja el token (P0)
                for (int i = 0; i < n; i++) {
                    if (pre[p][i] > 0) {
                        esInicio[i] = true;              // la que saca de esa plaza (T0)
                    }
                }
            }
        }

        // tiempos
        // actualizarTimestamps usa marcado y pre los caragmos antes
        this.tiempoMS = config.tiempoMS();
        this.sensibilizadaDesde = new long[n];
        Arrays.fill(sensibilizadaDesde, -1);   // al principio ninguna tiene momento anotado
        actualizarTimestamps(); // anotamos las que arrancan sensibilizadas (T0)

        // conflictos: los detectamos mirando la matriz Pre
        this.grupoDe = new int[n];
        Arrays.fill(grupoDe, -1);
        armarGruposDeConflicto(n);
        this.elegida = new int[grupos.size()];
        Arrays.fill(elegida, -1);
    }


    // fireTransition(t) es el metodo principal: a lo que llama cada hilo para disparar la transicion t
    @Override
    public boolean fireTransition(int t) {
        lock.lock();
        try {
            while (true) {
                while (!finished && !puedeDisparar(t)) {   // espera hasta que haya tokens
                    esperando[t]++;
                    colas[t].await();
                    esperando[t]--;
                }
                if (finished) {
                    return false;
                }
                long espera = tiempoRestante(t);   // cuanto le falta a t para poder dispararse
                if (espera <= 0) {
                    break;                         // ya paso el tiempo o es inmediata: salimos y disparamos
                }
                lock.unlock();                     // soltamos el lock para que los demas sigan
                try {
                    Thread.sleep(espera);          // dormimos lo que falta
                } finally {
                    lock.lock();                   // volvemos a tomar el lock antes de seguir
                }
                // vuelve al principio del while(true=
            }
            disparar(t);
            if (esInicio[t]) {
                disparosInicio++;
            }
            actualizarTimestamps();                // el disparo cambio el marcado actualizamos los momentos

            if (grupoDe[t] >= 0) {
                elegida[grupoDe[t]] = -1;   // se disparo la elegida: la proxima vez se vuelve a preguntar
            }

            verificarInvariantesPlaza();
            logger.logTransition(t);           // dentro del lock: el log queda en el orden real
            if (t == transicionFin) {          // se disparo T9
                disparosFin++;                 // un invariante completo mas
                if (disparosFin >= objetivo) { // llegamos a 200
                    finished = true;
                    fin.countDown();
                    despertarATodos();
                    return true;
                }
            }
            despertarSensibilizadas();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }

    //estaSensibilizada
    // recorre todas las plazas
    //ve si cada una tiene al menos los token que la transicion necesita
    // si ninguna falla sis esta sensibilizada
    private boolean estaSensibilizada(int t) {
        for (int p = 0; p < marcado.length; p++) {
            if (marcado[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }

    //disparar es la ecuacion para el estado
    //a cada plaza le suma lo que dice la columna t de la incidencia
    private void disparar(int t) {
        for (int p = 0; p < marcado.length; p++) {
            marcado[p] += incidencia[p][t];
        }
    }

    // despertarSensibilizadas
    // despues de disparar revisa las salas si una transicion quedo sensibilizada
    // y tiene hilos esperando despierta a uno
    private void despertarSensibilizadas() {
        for (int i = 0; i < esperando.length; i++) {
            if (esperando[i] > 0 && puedeDisparar(i)) {
                colas[i].signal();   // despierta a un solo hilo de la sala despues al siguiente es una cadena
            }
        }
    }

    // verificarInvariantesPlaza
    // para cada invariante suma los tokens de las plazas que le corresponden
    // (coeficiente 1 = suma esa plaza si es 0 = la ignora)
    // si alguna suma no da lanzamos una excepcion porque hay un error
    private void verificarInvariantesPlaza() {
        for (int i = 0; i < invariantesP.length; i++) { // recorremos los invariantes
            int suma = 0; //para cada invariante arranca una suma en cerp
            for (int p = 0; p < marcado.length; p++) { //recorre las 10 plazas
                suma += invariantesP[i][p] * marcado[p];
            }
            if (suma != sumInvariantesP[i]) {
                throw new IllegalStateException("Invariante de plaza " + i + " violado: suma = "
                        + suma + ", esperado = " + sumInvariantesP[i]);
            }
        }
    }

    // despertarATodos
// cuando el sistema termina, despierta a TODOS los hilos de TODAS las salas
// asi ninguno queda dormido para siempre
    private void despertarATodos() {
        for (Condition cola : colas) {
            cola.signalAll();
        }
    }
    // actualizarTimestamps
// para cada transicion: si acaba de quedar sensibilizada anotamos el momento,
// si dejo de estar sensibilizada lo borramos (-1)
    private void actualizarTimestamps() {
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < sensibilizadaDesde.length; i++) {
            if (estaSensibilizada(i)) {
                if (sensibilizadaDesde[i] < 0) {
                    sensibilizadaDesde[i] = ahora;
                }
            } else {
                sensibilizadaDesde[i] = -1;
            }
        }
    }

    // tiempoRestante
// cuantos ms le faltan a t para poder dispararse (0 o negativo = ya puede)
    private long tiempoRestante(int t) {
        if (tiempoMS[t] == 0) {
            return 0;                       // transicion inmediata
        }
        long transcurrido = System.currentTimeMillis() - sensibilizadaDesde[t];
        return tiempoMS[t] - transcurrido;
    }
    // armarGruposDeConflicto
// para cada plaza, busca que transiciones sacan tokens de ella
// si son 2 o mas, estan en conflicto y las juntamos en un grupo
    private void armarGruposDeConflicto(int n) {
        for (int p = 0; p < pre.length; p++) {
            List<Integer> salidas = new ArrayList<>();
            for (int t = 0; t < n; t++) {
                if (pre[p][t] > 0) {
                    salidas.add(t);
                }
            }
            if (salidas.size() < 2) {
                continue;                      // una sola salida: no hay conflicto en esta plaza
            }
            int g = -1;
            for (int t : salidas) {
                if (grupoDe[t] >= 0) {
                    g = grupoDe[t];            // alguna ya tiene grupo: usamos ese
                }
            }
            if (g < 0) {
                g = grupos.size();             // ninguna tiene grupo: creamos uno nuevo
                grupos.add(new ArrayList<>());
            }
            for (int t : salidas) {
                if (grupoDe[t] < 0) {
                    grupoDe[t] = g;
                    grupos.get(g).add(t);
                }
            }
        }
    }

    // puedeDisparar
// estaSensibilizada + la politica: si t esta en conflicto, solo puede disparar si la politica la eligio
    private boolean puedeDisparar(int t) {

        if (esInicio[t] && disparosInicio >= objetivo) {
            return false;   // ya entraron 200 transacciones: no se admiten mas
        }
        if (!estaSensibilizada(t)) {
            return false;                      // sin tokens no hay nada que decidir
        }
        int g = grupoDe[t];
        if (g < 0 || politica == null) {
            return true;                       // no esta en conflicto (o no hay politica): puede disparar
        }
        if (elegida[g] < 0) {
            boolean[] candidatas = new boolean[esperando.length];
            for (int x : grupos.get(g)) {
                candidatas[x] = true;
            }
            elegida[g] = politica.seleccionar(candidatas);
        }
        return elegida[g] == t;                //  solo dispara si es la elegida
    }
}