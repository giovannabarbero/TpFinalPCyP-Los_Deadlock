package ar.edu.fcefyn.pcp.tp2.publictests;

import ar.edu.fcefyn.pcp.tp2.app.Main;
import ar.edu.fcefyn.pcp.tp2.utilities.ResultadoValidacion;
import ar.edu.fcefyn.pcp.tp2.utilities.ValidadorRegex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SimulacionConcurrenciaTest {
    private static final Path DIR_RESULTADOS = Path.of("resultados");

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
        assertTrue(res.getConteoCamino2() > 0); // demostramos que se rompe el starvation
    }

    @Tag("estres")
    @RepeatedTest(value = 10, name = "Estrés corrida {currentRepetition} de {totalRepetitions}")
    @Timeout(value = 50, unit = TimeUnit.SECONDS)
    @DisplayName("Prueba de estrés repetitiva para descartar deadlocks bajo concurrencia")
    void testEstresSinDeadlocks() throws Exception {
        ejecutarYValidarSimulacion("ALEATORIA");
    }

    private ResultadoValidacion ejecutarYValidarSimulacion(String politica) throws Exception {
        String codigoPolitica = politica.equalsIgnoreCase("PRIORIZADA") ? "1" : "0";

        int hilosAntes = Thread.activeCount();
        long inicioNs = System.nanoTime();

        // Invocamos directamente a Main.main sin modificar Main.java
        Main.main(new String[]{codigoPolitica});

        long finNs = System.nanoTime();
        double duracionSegundos = (finNs - inicioNs) / 1_000_000_000.0;

        // Ver que dure entre 20 y 40s
        assertTrue(duracionSegundos >= 20.0 && duracionSegundos <= 40.0,
                String.format("Duración fuera de rango (%s): %.2f s (esperado: 20s - 40s)", politica, duracionSegundos));

        // Ver que no queden hilos activos
        Thread.sleep(100);
        assertEquals(hilosAntes, Thread.activeCount(),
                "Quedaron hilos activos sin finalizar después de terminar la simulación");

        // Buscamos el archivo de log recién generado por Main para esa política
        Path pathLog = obtenerUltimoLogGenerado(politica);
        String contenidoLog = Files.readString(pathLog);

        ValidadorRegex validador = new ValidadorRegex();
        ResultadoValidacion res = validador.validar(contenidoLog);
        assertTrue(res.esValido(), "Secuencia inválida en el log para política " + politica);
        assertEquals(200, res.getCantidadInvariantes(), "No se completaron los 200 invariantes");

        return res;
    }

    private Path obtenerUltimoLogGenerado(String politica) throws IOException {
        assertTrue(Files.exists(DIR_RESULTADOS), "No existe el directorio " + DIR_RESULTADOS);
        String prefijo = "disparos_" + politica.toUpperCase() + "_";

        try (Stream<Path> archivos = Files.list(DIR_RESULTADOS)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String nombre = p.getFileName().toString();
                        return nombre.startsWith(prefijo) && nombre.endsWith(".log");
                    })
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .orElseThrow(() -> new AssertionError(
                            "No se encontró ningún archivo de log con prefijo " + prefijo + " en " + DIR_RESULTADOS));
        }
    }
}