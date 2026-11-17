// Guardamos cuantos tokens hay en cada plaza y si un hilo pide disparar, revisa si se puede
// Y se asegura de que dos hilos nunca toquen los tokens al mismo tiempo

package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import ar.edu.fcefyn.pcp.tp2.api.TransitionLogger;

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
    private final int transicionFin;       // que transicion marca un invariante completo (T9)
    private final int objetivo;          // cuantas veces tiene que dispararse (200)
    private int disparosFin = 0;           // cuantas veces se disparo hasta ahora
    private boolean finished = false;       // true = el sistema termino

    //constructor
    //el monitor es agnostico solo recibe numeros si cambiamos la red cambiaria la configuracion y el Monitor queda igual
    public Monitor(PetriNetConfig config, Politica politica, TransitionLogger logger) {
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

        // la condicion de fin viene de config
        this.transicionFin = config.transicionFinInvariantes();
        this.objetivo = config.objetivoInvariantes();
    }


    // fireTransition(t) es el metodo principal: a lo que llama cada hilo para disparar la transicion t
    @Override
    public boolean fireTransition(int t) {
        lock.lock();
        try {
            while (!finished && !estaSensibilizada(t)) { // espera solo si NO termino y NO puede disparar
                esperando[t]++;
                colas[t].await();
                esperando[t]--;
            }
            if (finished) {
                return false;                  // el sistema termino: el hilo tiene que salir
            }
            disparar(t);
            verificarInvariantesPlaza();
            logger.logTransition(t);           // dentro del lock: el log queda en el orden real
            if (t == transicionFin) {          // se disparo T9
                disparosFin++;                 // un invariante completo mas
                if (disparosFin >= objetivo) { // llegamos a 200
                    finished = true;
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
            if (esperando[i] > 0 && estaSensibilizada(i)) {
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
}