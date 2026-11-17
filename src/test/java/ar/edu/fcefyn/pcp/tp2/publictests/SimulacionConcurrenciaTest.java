package ar.edu.fcefyn.pcp.tp2.publictests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.fcefyn.pcp.tp2.utilities.ResultadoValidacion;
import ar.edu.fcefyn.pcp.tp2.utilities.ValidadorRegex;

public class SimulacionConcurrenciaTest {
    private static final String RUTA_LOG = "resultados/log_disparos.txt";

    @Test
    @Timeout(value = 50, unit = TimeUnit.SECONDS)
    @DisplayName("Ejecución completa con Política Aleatoria: 200 invariantes, 20-40s y sin hilos huérfanos")
    void testPoliticaAleatoria() throws Exception {
        ejecutarYValidarSimulacion("ALEATORIA");
    }

    @Test
    @Timeout(value = 50, unit = TimeUnit.SECONDS)
    @DisplayName("Ejecución completa con Política Priorizada (alto riesgo): 200 invariantes y 20-40s")
    void testPoliticaPriorizada() throws Exception {
        ResultadoValidacion res = ejecutarYValidarSimulacion("PRIORIZADA");
        assertTrue(res.getConteoCamino2() > 0); //demostramos que se rompe el starvation
    }

    @Tag("estres")
    @RepeatedTest(value = 10, name = "Estrés corrida {currentRepetition} de {totalRepetitions}")
    @Timeout(value = 50, unit = TimeUnit.SECONDS)
    @DisplayName("Prueba de estrés repetitiva para descartar deadlocks bajo concurrencia")
    void testEstresSinDeadlocks() throws Exception {
        ejecutarYValidarSimulacion("ALEATORIA");
    }

    private ResultadoValidacion ejecutarYValidarSimulacion(String politica) throws Exception {
        int hilosAntes = Thread.activeCount();
        long inicioNs = System.nanoTime();


        // Ejemplo: Main.ejecutarSimulacion(politica);
        //Invocar el punto de entrada de la simulación con la política indicada
        long finNs = System.nanoTime();
        double duracionSegundos = (finNs - inicioNs) / 1_000_000_000.0;

        // Ver que dure entre 20 y 40s
        assertTrue(duracionSegundos >= 20.0 && duracionSegundos <= 40.0,
                String.format("Duración fuera de rango (%s): %.2f s (esperado: 20s - 40s)", politica, duracionSegundos));

        // Ver que no queden hilos activos
        Thread.sleep(100); // Les damos un tiempito
        assertEquals(hilosAntes, Thread.activeCount(),
                "Quedaron hilos activos sin finalizar después de terminar la simulación");

        // Auditamos el archivo de log con el validador Regex (200 T-invariantes)
        Path pathLog = Path.of(RUTA_LOG);
        assertTrue(Files.exists(pathLog), "No se generó el archivo de log en " + RUTA_LOG);
        String contenidoLog = Files.readString(pathLog);


        ValidadorRegex validador = new ValidadorRegex();
         ResultadoValidacion res = validador.validar(contenidoLog);
         assertTrue(res.esValido(), "Secuencia inválida en el log para política " + politica);
         assertEquals(200, res.getCantidadInvariantes(), "No se completaron los 200 invariantes");

         return res;
    }
}
