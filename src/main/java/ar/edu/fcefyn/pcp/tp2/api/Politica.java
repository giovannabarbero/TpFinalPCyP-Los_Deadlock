package ar.edu.fcefyn.pcp.tp2.api;

public interface Politica {

    /**
     * despertables[t] = true si la transición t
     * está sensibilizada Y tiene hilos esperando.
     *
     * @return transición elegida o -1 si no hay ninguna.
     */
    int seleccionar(boolean[] despertables);
}
