package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaAleatoria implements Politica {

    @Override
    public int seleccionar(boolean[] despertables) {
        List<Integer> candidatas = new ArrayList<>();
        for (int t = 0; t < despertables.length; t++) {
            if (despertables[t]) {
                candidatas.add(t);
            }
        }
        if (candidatas.isEmpty()) {
            return -1;
        }
        return candidatas.get(ThreadLocalRandom.current().nextInt(candidatas.size()));
    }
}
