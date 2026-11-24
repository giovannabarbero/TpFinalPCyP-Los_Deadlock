package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaPriorizada implements Politica {
    private final int[] pesosPrio;

    public PoliticaPriorizada(int[] pesosPrio) {
        this.pesosPrio = pesosPrio.clone();
    }

    // Elige la despertable de mayor peso (T4 en el config); si empatan, al azar
    @Override
    public int seleccionar(boolean[] despertables) {
        int pesoMax = -1;
        List<Integer> mejores = new ArrayList<>();
        for (int t = 0; t < despertables.length; t++) {
            if (!despertables[t]) {
                continue;
            }
            if (pesosPrio[t] > pesoMax) {
                pesoMax = pesosPrio[t];
                mejores.clear();
            }
            if (pesosPrio[t] == pesoMax) {
                mejores.add(t);
            }
        }
        if (mejores.isEmpty()) {
            return -1;
        }
        return mejores.get(ThreadLocalRandom.current().nextInt(mejores.size()));
    }
}