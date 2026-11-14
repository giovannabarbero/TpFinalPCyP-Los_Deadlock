package ar.edu.fcefyn.pcp.tp2.api;

import java.util.List;

public interface Politica {
    /*
     * Recibe T sensibilizada con hilos esperando y devuelve cual debe dispararse, recibe pesos por config
     *
     *  @param transicionesCand lista de indices de transiciones en conflicto
     *
     * @return indice de la transicion elegida
     */
    int seleccionar(List<Integer> transicionesCand);
}
