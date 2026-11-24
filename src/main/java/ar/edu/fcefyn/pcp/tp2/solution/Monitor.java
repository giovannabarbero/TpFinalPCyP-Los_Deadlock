package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;
import ar.edu.fcefyn.pcp.tp2.api.Politica;
import ar.edu.fcefyn.pcp.tp2.api.TransitionLogger;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Monitor implements MonitorInterface {

    private final int[][] pre;
    private final int[][] incidencia;
    private final int[] marcado;
    private final int[][] invariantesP;
    private final int[] sumInvariantesP;

    private final Semaphore mutex = new Semaphore(1, true);
    private final Semaphore[] colas;
    private final int[] esperando;

    private final long[] tiempoMS;
    private final long[] sensibilizadaDesde;
    private final boolean[] esperandoVentana;

    private final Politica politica;
    private final TransitionLogger logger;
    private final CountDownLatch fin;
    private final int transicionFin;
    private final int objetivo;
    private final boolean[] esInicio;
    private int disparosInicio = 0;
    private int disparosFin = 0;
    private boolean finished = false;

    public Monitor(PetriNetConfig config, Politica politica, TransitionLogger logger, CountDownLatch fin) {
        this.pre = config.matrixPre();
        this.incidencia = config.matrixIncidencia();
        this.marcado = config.marcadoInicial().clone();
        this.invariantesP = config.invariantesP();
        this.sumInvariantesP = config.sumInvariantesP();

        int n = config.numTransiciones();
        this.colas = new Semaphore[n];
        for (int i = 0; i < n; i++) {
            colas[i] = new Semaphore(0, true);
        }
        this.esperando = new int[n];

        this.politica = politica;
        this.logger = logger;
        this.fin = fin;
        this.transicionFin = config.transicionFinInvariantes();
        this.objetivo = config.objetivoInvariantes();

        // transicion de entrada (T0): saca tokens de la plaza donde la de fin devuelve el token
        this.esInicio = new boolean[n];
        for (int p = 0; p < pre.length; p++) {
            if (incidencia[p][transicionFin] > 0) {
                for (int i = 0; i < n; i++) {
                    if (pre[p][i] > 0) {
                        esInicio[i] = true;
                    }
                }
            }
        }

        this.tiempoMS = config.tiempoMS();
        this.sensibilizadaDesde = new long[n];
        Arrays.fill(sensibilizadaDesde, -1);
        this.esperandoVentana = new boolean[n];
        actualizarTimestamps();
    }

    @Override
    public boolean fireTransition(int t) {
        if (!tomarMutex()) {
            return false;
        }
        while (true) {
            if (finished) {
                mutex.release();
                return false;
            }
            if (esperandoVentana[t] || !puedeDisparar(t)) {
                if (!esperarEnCola(t)) {
                    return false;
                }
                continue;                        // lo despertaron, ya tiene el mutex
            }
            long espera = tiempoRestante(t);
            if (espera > 0) {
                if (!esperarVentana(t, espera)) {
                    return false;
                }
                continue;
            }
            break;
        }

        try {
            disparar(t);
            if (esInicio[t]) {
                disparosInicio++;
            }
            actualizarTimestamps();
            verificarInvariantesPlaza();
            logger.logTransition(t);
        } catch (RuntimeException e) {
            mutex.release();
            throw e;
        }

        if (t == transicionFin) {
            disparosFin++;
            if (disparosFin >= objetivo) {
                finished = true;
                fin.countDown();
                mutex.release();
                return true;
            }
        }
        senalizarSiguiente();
        return true;
    }

    // Unica llamada a la politica despues de un disparo exitoso
    private void senalizarSiguiente() {
        boolean[] despertables = new boolean[esperando.length];
        int cantidad = 0;
        for (int i = 0; i < despertables.length; i++) {
            despertables[i] = esperando[i] > 0 && puedeDisparar(i) && !esperandoVentana[i];
            if (despertables[i]) {
                cantidad++;
            }
        }
        int elegida = politica.seleccionar(despertables);
        if (cantidad >= 2) {
            logger.logDecision(despertables, elegida);
        }
        if (elegida >= 0) {
            esperando[elegida]--;
            colas[elegida].release();            // signal and exit, no se libera el mutex
        } else {
            mutex.release();
        }
    }

    private boolean esperarEnCola(int t) {
        esperando[t]++;
        mutex.release();
        try {
            colas[t].acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean esperarVentana(int t, long espera) {
        esperandoVentana[t] = true;              // para que nadie se cuele a disparar t
        mutex.release();
        try {
            Thread.sleep(espera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!tomarMutex()) {
            return false;
        }
        esperandoVentana[t] = false;
        return true;
    }

    private boolean tomarMutex() {
        try {
            mutex.acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean puedeDisparar(int t) {
        if (esInicio[t] && disparosInicio >= objetivo) {
            return false;
        }
        return estaSensibilizada(t);
    }

    private boolean estaSensibilizada(int t) {
        for (int p = 0; p < marcado.length; p++) {
            if (marcado[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }

    // M' = M + I * sigma
    private void disparar(int t) {
        for (int p = 0; p < marcado.length; p++) {
            marcado[p] += incidencia[p][t];
        }
    }

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

    private long tiempoRestante(int t) {
        if (tiempoMS[t] == 0) {
            return 0;
        }
        return tiempoMS[t] - (System.currentTimeMillis() - sensibilizadaDesde[t]);
    }

    private void verificarInvariantesPlaza() {
        for (int i = 0; i < invariantesP.length; i++) {
            int suma = 0;
            for (int p = 0; p < marcado.length; p++) {
                suma += invariantesP[i][p] * marcado[p];
            }
            if (suma != sumInvariantesP[i]) {
                throw new IllegalStateException("Invariante de plaza " + i + " violado: suma = "
                        + suma + ", esperado = " + sumInvariantesP[i]);
            }
        }
    }
}