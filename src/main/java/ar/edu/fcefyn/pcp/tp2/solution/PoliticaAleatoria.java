package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaAleatoria implements Politica {

    @Override
    public int seleccionar(boolean[] despertables) {

        if (despertables == null) {
            throw new IllegalArgumentException(
                "El vector de transiciones no puede ser null"
            );
        }

        List<Integer> candidatas = new ArrayList<>();

        // Buscamos las transiciones que pueden despertarse
        for (int t = 0; t < despertables.length; t++) {
            if (despertables[t]) {
                candidatas.add(t);
            }
        }

        // No hay ningún hilo para despertar
        if (candidatas.isEmpty()) {
            return -1;
        }

        // Elegimos una candidata al azar
        int indice = ThreadLocalRandom.current()
                .nextInt(candidatas.size());

        return candidatas.get(indice);
    }
}
