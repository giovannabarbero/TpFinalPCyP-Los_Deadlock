package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaPriorizada implements Politica {
    private final int[] pesosPrio;

    public PoliticaPriorizada(int[] pesosPrio) {
        if (pesosPrio == null || pesosPrio.length != 10) {
            throw new IllegalArgumentException ("Se necesitan 10 pesos, uno por transición");
        }

        this.pesosPrio = pesosPrio.clone();

        for(int peso : this.pesosPrio){
            if(peso<0){
                throw new IllegalArgumentException("Los pesos no pueden ser negativos");
            }
        }
    }

    @Override
    public int seleccionar(List<Integer> transicionesCand){
        if (transicionesCand == null || transicionesCand.isEmpty()){
            throw new IllegalArgumentException ("No hay transiciones candidatas");
        }

        long total = 0;

        // Suma de los pesos de las transiciones candidatas
        for(int transicion : transicionesCand){
            if(transicion<0 || transicion>=pesosPrio.length){
                throw new IllegalArgumentException(
                    "Indice de transición inválido: " + transicion);
            }

            total += pesosPrio[transicion];
        }

        if(total <= 0){
            throw new IllegalArgumentException(
                "La suma de los pesos debe ser mayor que cero");
        }

        // Sorteo de numero segun el peso total
        long sorteo = ThreadLocalRandom.current().nextLong(total);
        // Transición correspondiente al peso
        for(int transicion : transicionesCand){
            sorteo -= pesosPrio[transicion];
            
            if(sorteo < 0) {
                return transicion;
            }
        }

        throw new IllegalStateException("No se pudo seleccionar una transición");
    }
}
