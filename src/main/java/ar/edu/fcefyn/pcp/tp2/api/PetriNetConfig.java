package ar.edu.fcefyn.pcp.tp2.api;

public record PetriNetConfig(
        int numPlazas,
        int numTransiciones,
        int[][] matrixPre,              // I-
        int[][] matrixIncidencia,       // I
        int[] marcadoInicial,           // M_0
        long[] tiempoMS,                // 0 immediate, >0 temp
        int[] pesosPrio,                // Peso para politica a priorizar
        int[][] invariantesP,           // Eq de invariants plaza
        int[] sumInvariantesP,          // Const. esperadas de c/d eq
        int transicionFinInvariantes,   // indice T9
        int objetivoInvariantes,        // 200 inv por exe
        int tipoPolitica                // 0-> Random, 1-> Prio
) {
}
