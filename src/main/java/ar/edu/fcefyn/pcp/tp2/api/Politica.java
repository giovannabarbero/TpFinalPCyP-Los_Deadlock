package ar.edu.fcefyn.pcp.tp2.api;

public interface Politica {
    /*
     * Se llama UNA sola vez, despues de cada disparo exitoso.
     *
     * @param despertables despertables[t] = true si t esta sensibilizada Y tiene hilos esperando
     * @return la transicion elegida para despertar, o -1 si no hay ninguna
     */
    int seleccionar(boolean[] despertables);
}