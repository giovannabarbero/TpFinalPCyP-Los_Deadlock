
package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.Politica;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoliticaPriorizada implements Politica {

    private final int[] pesosPrio;

    public PoliticaPriorizada(int[] pesosPrio) {
if (pesosPrio == null || pesosPrio.length == 0) {
    throw new IllegalArgumentException(
        "Los pesos de prioridad son inválidos"
    );
}

this.pesosPrio = pesosPrio.clone();

for (int peso : this.pesosPrio) {
    if (peso < 0) {
        throw new IllegalArgumentException(
            "Los pesos no pueden ser negativos"
        );
    }
}
    }

    // Elige la despertable de mayor peso (T4 en el config); si empatan, al azar
    @Override
    public int seleccionar(boolean[] despertables) {
// Elige la despertable de mayor peso; si empatan, al azar
@Override
public int seleccionar(boolean[] despertables) {

    if (despertables == null ||
        despertables.length != pesosPrio.length) {
        throw new IllegalArgumentException(
            "El vector de transiciones tiene una dimensión inválida"
        );
    }

    int mayorPeso = -1;
    List<Integer> mejores = new ArrayList<>();

    // Buscamos la candidata con mayor prioridad
    for (int t = 0; t < despertables.length; t++) {

        if (despertables[t]) {

            if (pesosPrio[t] > mayorPeso) {
                mayorPeso = pesosPrio[t];
                mejores.clear();
                mejores.add(t);

            } else if (pesosPrio[t] == mayorPeso) {
                mejores.add(t);
            }
        }
    }

    // No hay transiciones para despertar
    if (mejores.isEmpty()) {
        return -1;
    }

    // Si hay empate, elegimos aleatoriamente
    int indice = ThreadLocalRandom.current()
            .nextInt(mejores.size());

    return mejores.get(indice);
}
    }
}