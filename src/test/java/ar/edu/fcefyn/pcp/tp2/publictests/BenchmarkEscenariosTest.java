package publictests;

import ar.edu.fcefyn.pcp.tp2.app.Main;
import ar.edu.fcefyn.pcp.tp2.utilities.ResultadoValidacion;
import ar.edu.fcefyn.pcp.tp2.utilities.ValidadorRegex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("benchmark")
public class BenchmarkEscenariosTest {

    private static final Path CONFIG_PATH = Path.of("config/config.properties");
    private static final Path RESULTADOS_DIR = Path.of("resultados");

    record Escenario(String nombre, String tiemposMs, int d1, int d2, int d3) {}

    @Test
    @DisplayName("Ejecutar los 3 escenarios temporales con ambas políticas y calcular tabla")
    void correrTresEscenarios() throws Exception {
        String configOriginal = Files.readString(CONFIG_PATH);

        List<Escenario> escenarios = List.of(
                new Escenario("1. Base", "0,0,85,85,0,210,0,95,95,0", 170, 210, 190),
                new Escenario("2. Alto riesgo pesado", "0,0,75,75,0,225,0,85,85,0", 150, 240, 170),
                new Escenario("3. Laterales pesadas", "0,0,120,120,0,140,0,130,130,0", 240, 140, 260)
        );

        try {
            for (Escenario esc : escenarios) {
                for (int tipoPolitica : List.of(0, 1)) { // 0 = Aleatoria, 1 = Priorizada
                    String nombrePol = (tipoPolitica == 0) ? "Aleatoria" : "Priorizada";

                    // Actualizar tiempos, política y peso=3 en config.properties
                    String configModificada = configOriginal
                            .replaceAll("red\\.tiempos\\.ms=.*", "red.tiempos.ms=" + esc.tiemposMs())
                            .replaceAll("politica\\.tipo=.*", "politica.tipo=" + tipoPolitica)
                            .replaceAll("politica\\.pesos=.*", "politica.pesos=1,1,1,1,3,1,1,1,1,1");
                    Files.writeString(CONFIG_PATH, configModificada);

                    long inicioNs = System.nanoTime();
                    Main.main(new String[]{});
                    double tPracticoSeg = (System.nanoTime() - inicioNs) / 1_000_000_000.0;

                    // Buscar el archivo .txt más reciente en resultados/
                    Path ultimoLog = Files.list(RESULTADOS_DIR)
                            .filter(p -> p.getFileName().toString().startsWith("disparos_") && p.toString().endsWith(".log"))
                            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                            .orElseThrow(() -> new AssertionError("No se encontró log de disparos en resultados/"));

                    ValidadorRegex validador = new ValidadorRegex();
                    ResultadoValidacion res = validador.validar(Files.readString(ultimoLog));
                    assertTrue(res.esValido(), "Log inválido en " + esc.nombre() + " - " + nombrePol + ": " + res.getMensaje());
                    assertEquals(200, res.getCantidadInvariantes());

                    int n1 = res.getConteoCamino1();
                    int n4 = res.getConteoCamino2();
                    int n6 = res.getConteoCamino3();
                    double tIdealSeg = (n4 * esc.d2() + Math.max(n1 * esc.d1(), n6 * esc.d3())) / 1000.0;
                    double overheadSeg = tPracticoSeg - tIdealSeg;

                    System.out.printf("| %s | %s | N=(%d, %d, %d) | T_ideal=%.2f s | T_practico=%.2f s | Overhead=+%.2f s |%n",
                            esc.nombre(), nombrePol, n1, n4, n6, tIdealSeg, tPracticoSeg, overheadSeg);
                }
            }
        } finally {
            // Restaurar siempre el config.properties original
            Files.writeString(CONFIG_PATH, configOriginal);
        }
    }
}