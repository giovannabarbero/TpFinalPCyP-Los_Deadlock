// Guardamos cuantos tokens hay en cada plaza y si un hilo pide disparar, revisa si se puede
// Y se asegura de que dos hilos nunca toquen los tokens al mismo tiempo

package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Monitor implements MonitorInterface {

    //atributos
    //int[][] es una matriz
    private final int[][] pre;          // I-: cuantos tokens consume cada transicion de cada plaza
    private final int[][] incidencia;   // I = I+ - I- cuanto cambia cada plaza cuando se dispara una transicion
    private final int[] marcado;        // tokens actuales en cada plaza
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition[] colas;   // una sala de espera por transición
    private final int[] esperando;     // cuantos hilos hay en cada sala

    //constructor
    //el monitor es agnostico solo recibe numeros si cambiamos la red cambiaria la configuracion y el Monitor queda igual
    public Monitor(PetriNetConfig config) {
        this.pre = config.matrixPre();
        this.incidencia = config.matrixIncidencia();
        this.marcado = config.marcadoInicial().clone();
        int n = config.numTransiciones();
        this.colas = new Condition[n];
        for (int i = 0; i < n; i++) {
            colas[i] = lock.newCondition();   // cada sala esta atada a este lock
        }
        this.esperando = new int[n];

    }

    // fireTransition(t) es el metodo principal: a lo que llama cada hilo para disparar la transicion t
    @Override
    public boolean fireTransition(int t) {
        lock.lock(); // toma el lock, si lo tiene otro espera
        try {
            while (!estaSensibilizada(t)) { // aca ponemos un while por que vuelve a chequear si hay token
                esperando[t]++;        // se anota en la sala de t, para llevar la cuenta
                colas[t].await();      // duerme (y suelta el lock)
                esperando[t]--;        // se despierta: se borrs de la lista
            }
            disparar(t);
            despertarSensibilizadas();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock(); // deja el lock
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
    private void despertarSensibilizadas() {
        for (int i = 0; i < esperando.length; i++) {
            if (esperando[i] > 0 && estaSensibilizada(i)) {
                colas[i].signal();   // despierta a un solo hilo de la sala despues al siguiente es una cadena
            }
        }
    }
}