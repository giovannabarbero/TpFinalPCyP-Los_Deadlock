package ar.edu.fcefyn.pcp.tp2.publictests;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.fcefyn.pcp.tp2.utilities.ResultadoValidacion;
import ar.edu.fcefyn.pcp.tp2.utilities.ValidadorRegex;

public class ValidadorInvariantesTest {
    private ValidadorRegex validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorRegex();
    }

    @Test
    @DisplayName("Debe aprobar un log secuencial válido con exactamente 200 T-invariantes")
    void testLogSecuencialValido() {
        StringBuilder logBuilder = new StringBuilder();
        String[] invariantes = {
                "T0 T1 T2 T3 T9 ",
                "T0 T4 T5 T9 ",
                "T0 T6 T7 T8 T9 "
        };

        // 200 invariantes
        for (int i = 0; i < 200; i++) {
            logBuilder.append(invariantes[i % 3]);
        }

        String logSimulado = logBuilder.toString().trim();


         ResultadoValidacion resultado = validador.validar(logSimulado);
         assertTrue(resultado.esValido(), "El log con 200 invariantes secuenciales debería ser válido");
         assertEquals(200, resultado.getCantidadInvariantes());
        assertFalse(logSimulado.isEmpty());
    }

    @Test
    @DisplayName("Debe aprobar disparos concurrentes intercalados mientras formen 200 invariantes")
    void testLogConcurrenteIntercalado() {
        //  2 invariantes
        // Camino A: T0 -> T1 -> T2 -> T3 -> T9
        // Camino B: T0 -> T4 -> T5 -> T9
        String bloqueIntercalado = "T0 T0 T1 T4 T2 T5 T3 T9 T9 ";

        //  100 veces = 200 invariantes
        String logIntercalado = bloqueIntercalado.repeat(100).trim();


        ResultadoValidacion resultado = validador.validar(logIntercalado);
        assertTrue(resultado.esValido(), "El validador debe consumir invariantes no contiguos");
        assertEquals(200, resultado.getCantidadInvariantes());
        assertEquals(900, logIntercalado.split(" ").length);
    }

    @Test
    @DisplayName("Debe fallar si quedan transiciones sueltas sin completar un T-invariante")
    void testLogIncompletoFalla() {
        StringBuilder logBuilder = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            logBuilder.append("T0 T4 T5 T9 ");
        }
        // Agregamos un invariante
        logBuilder.append("T0 T1 T2");

        String logCorrupto = logBuilder.toString();


        assertFalse(validador.validar(logCorrupto).esValido(),
             "Debe detectar transiciones residuales que no cerraron invariante");
        assertTrue(logCorrupto.endsWith("T2"));
    }

    @Test
    @DisplayName("Debe fallar si la cantidad de invariantes completos es distinta de 200")
    void testCantidadErroneaInvariantesFalla() {
        //  199 invariantes válidos
        String logIncompleto = "T0 T4 T5 T9 ".repeat(199).trim();


        assertFalse(validador.validar(logIncompleto).esValido(),
            "Debe fallar si hay 199 invariantes en lugar de 200");
        assertEquals(199 * 4, logIncompleto.split(" ").length);
    }
}
