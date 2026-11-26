package ar.edu.fcefyn.pcp.tp2.publictests;

import ar.edu.fcefyn.pcp.tp2.utilities.ResultadoValidacion;
import ar.edu.fcefyn.pcp.tp2.utilities.ValidadorRegex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidadorInvariantesTest {

    private ValidadorRegex validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorRegex();
    }

    /**
     * Genera un log válido con exactamente 200 T-invariantes rotando los 3 caminos.
     */
    private String generarLogValido200() {
        StringBuilder sb = new StringBuilder();
        String[] invariantes = {
                "T0 T1 T2 T3 T9 ",
                "T0 T4 T5 T9 ",
                "T0 T6 T7 T8 T9 "
        };
        for (int i = 0; i < 200; i++) {
            sb.append(invariantes[i % 3]);
        }
        return sb.toString().trim();
    }

    // ==================== CASOS VÁLIDOS ====================

    @Test
    @DisplayName("Debe aprobar un log secuencial válido con exactamente 200 T-invariantes")
    void testLogSecuencialValido() {
        String logSimulado = generarLogValido200();

        ResultadoValidacion resultado = validador.validar(logSimulado);

        assertTrue(resultado.esValido(), "El log con 200 invariantes secuenciales debería ser válido");
        assertEquals(200, resultado.getCantidadInvariantes());
        assertEquals(67, resultado.getConteoCamino1()); // i % 3 == 0 (0..198 -> 67)
        assertEquals(67, resultado.getConteoCamino2()); // i % 3 == 1 (1..199 -> 67)
        assertEquals(66, resultado.getConteoCamino3()); // i % 3 == 2 (2..197 -> 66)
    }

    @Test
    @DisplayName("Debe aprobar disparos concurrentes intercalados mientras formen 200 invariantes")
    void testLogConcurrenteIntercalado() {
        // Simula 2 invariantes mezclados en paralelo (Camino 1 y Camino 2)
        String bloqueIntercalado = "T0 T0 T1 T4 T2 T5 T3 T9 T9 ";
        String logIntercalado = bloqueIntercalado.repeat(100).trim();

        ResultadoValidacion resultado = validador.validar(logIntercalado);

        assertTrue(resultado.esValido(), "El validador debe consumir invariantes intercalados no contiguos");
        assertEquals(200, resultado.getCantidadInvariantes());
        assertEquals(100, resultado.getConteoCamino1());
        assertEquals(100, resultado.getConteoCamino2());
    }

    // ==================== LOGS ROTOS A MANO (DEBEN FALLAR) ====================

    @Test
    @DisplayName("Debe fallar si se elimina una transición intermedia (log roto a mano)")
    void testLogRotoFaltaTransicion() {
        String logBase = generarLogValido200();
        // Rompemos un ciclo borrando la primera ocurrencia de T2 (queda T0 T1 T3 T9)
        String logRoto = logBase.replaceFirst("\\bT2\\b\\s*", "");

        ResultadoValidacion resultado = validador.validar(logRoto);

        assertFalse(resultado.esValido(), "Debe fallar si falta una transición intermedia como T2");
        assertFalse(resultado.getRemanente().isEmpty(), "Deben quedar transiciones huérfanas sin consumir");
    }

    @Test
    @DisplayName("Debe fallar si se invierte el orden causal de disparos (T2 antes que T1)")
    void testLogRotoOrdenInvertido() {
        String logBase = generarLogValido200();
        // Invertimos el orden causal en el primer invariante: T0 T2 T1 T3 T9
        String logRoto = logBase.replaceFirst("T0 T1 T2 T3 T9", "T0 T2 T1 T3 T9");

        ResultadoValidacion resultado = validador.validar(logRoto);

        assertFalse(resultado.esValido(), "Debe detectar la violación de orden causal T2 antes que T1");
        assertFalse(resultado.getRemanente().isEmpty());
    }

    @Test
    @DisplayName("Debe fallar si se agrega una transición suelta extra")
    void testLogRotoTransicionSuelta() {
        // Agregamos un T4 suelto en el medio de un log de 200 invariantes válidos
        String logBase = generarLogValido200();
        String logRoto = logBase.replaceFirst("T0 T4 T5 T9", "T0 T4 T4 T5 T9");

        ResultadoValidacion resultado = validador.validar(logRoto);

        assertFalse(resultado.esValido(), "Debe detectar la transición suelta T4 extra");
        assertTrue(resultado.getRemanente().contains("T4"));
    }

    @Test
    @DisplayName("Debe fallar si quedan transiciones de un ciclo truncado al final")
    void testLogIncompletoFalla() {
        String logCorrupto = generarLogValido200() + " T0 T1 T2";

        ResultadoValidacion resultado = validador.validar(logCorrupto);

        assertFalse(resultado.esValido(), "Debe detectar transiciones residuales que no cerraron invariante");
        assertEquals("T0 T1 T2", resultado.getRemanente());
    }

    @Test
    @DisplayName("Debe fallar si la cantidad de invariantes completos es distinta de 200")
    void testCantidadErroneaInvariantesFalla() {
        String log199 = "T0 T4 T5 T9 ".repeat(199).trim();
        String log201 = "T0 T4 T5 T9 ".repeat(201).trim();

        assertFalse(validador.validar(log199).esValido(), "Debe fallar con 199 invariantes");
        assertFalse(validador.validar(log201).esValido(), "Debe fallar con 201 invariantes");
    }

    @Test
    @DisplayName("Debe fallar si el log está vacío o nulo")
    void testLogVacioONuloFalla() {
        assertFalse(validador.validar("").esValido());
        assertFalse(validador.validar("   ").esValido());
        assertFalse(validador.validar(null).esValido());
    }
}