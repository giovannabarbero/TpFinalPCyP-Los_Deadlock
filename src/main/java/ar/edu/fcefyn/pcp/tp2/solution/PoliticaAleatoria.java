package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaAleatoria implements Politica {
    
    @Override
    public int seleccionar(List<Integer> transicionesCand) {
        if (transicionesCand == null || transicionesCand.isEmpty()) {
            throw new IllegalArgumentException (
                "No hay transiciones candidatas"
            );
        }

        int indice = ThreadLocalRandom.current().nextInt(transicionesCand.size());

        return transicionesCand.get(indice);
    }
}
