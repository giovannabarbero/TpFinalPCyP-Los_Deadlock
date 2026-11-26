package ar.edu.fcefyn.pcp.tp2.api;

public interface Politica {


    /**
     * Se llama UNA sola vez, después de cada disparo exitoso.
     *
     * @param despertables despertables[t] = true si t está
     * sensibilizada Y tiene hilos esperando.
     *
     * @return la transición elegida para despertar,
     * o -1 si no hay ninguna.
     */
    int seleccionar(boolean[] despertables);

}
