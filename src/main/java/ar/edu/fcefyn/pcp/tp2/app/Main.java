package ar.edu.fcefyn.pcp.tp2.app;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;
// Clara: Descomentar cuando se suban las clases de política a solution
// import ar.edu.fcefyn.pcp.tp2.api.Politica;
// import ar.edu.fcefyn.pcp.tp2.solution.PoliticaAleatoria;
// import ar.edu.fcefyn.pcp.tp2.solution.PoliticaPriorizada;
import ar.edu.fcefyn.pcp.tp2.solution.FileTransitionLogger;
import ar.edu.fcefyn.pcp.tp2.solution.Monitor;
import ar.edu.fcefyn.pcp.tp2.solution.SegmentWorker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Main {

    public static final int POLITICA_ALEATORIA = 0;
    public static final int POLITICA_PRIORIZADA = 1;

    public static void main(String[] args) throws Exception {
        // Relative pathing
        Path rutaConfig = Path.of("config", "config.properties");
        Integer politicaPorConsola = (args.length > 0) ? Integer.parseInt(args[0].trim()) : null;

        PetriNetConfig config = cargarConfiguracion(rutaConfig, politicaPorConsola);
        int codigoPolitica = config.tipoPolitica();
        String nombrePolitica = obtenerNombrePolitica(codigoPolitica);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path rutaLogDisparos = Path.of("resultados", "disparos_" + nombrePolitica + "_" + timestamp + ".log");
        Path rutaResumen = Path.of("resultados", "resumen_" + nombrePolitica + "_" + timestamp + ".txt");

        FileTransitionLogger logger = new FileTransitionLogger(rutaLogDisparos);
        List<Thread> hilos = new ArrayList<>(7);
        long inicioNs;
        long finNs;

        try {
            // Guille & Clara
            // 1. ahora compila con el Monitor v1 actual (solo recibe config).
            // 2. cuando suban sus ramas, comentar la línea de abajo y
            //    descomentar el bloque de politica + ponitor completo.
            MonitorInterface monitor = new Monitor(config);

            /*
            Politica politica = (codigoPolitica == POLITICA_PRIORIZADA)
                    ? new PoliticaPriorizada(config.pesosPrio())
                    : new PoliticaAleatoria();
            MonitorInterface monitor = new Monitor(config, politica, logger);
            */

            hilos = crearHilosDeSegmentos(monitor);

            System.out.printf("Iniciando simulación PSP | Política: %d (%s) | Objetivo: %d invariantes | Hilos: %d%n",
                    codigoPolitica, nombrePolitica, config.objetivoInvariantes(), hilos.size());

            inicioNs = System.nanoTime();

            for (Thread hilo : hilos) {
                hilo.start();
            }

            // Espera a que todos los hilos finalicen ordenadamente (Requerimiento 3)
            for (Thread hilo : hilos) {
                hilo.join();
            }

            finNs = System.nanoTime();
        } finally {
            logger.close();
        }

        verificarFinalizacionHilos(hilos);

        double duracionSegundos = (finNs - inicioNs) / 1_000_000_000.0;
        guardarResumenEjecucion(
                rutaResumen,
                codigoPolitica,
                nombrePolitica,
                hilos,
                duracionSegundos,
                logger.getTotalDisparos(),
                rutaLogDisparos
        );
    }

    private static String obtenerNombrePolitica(int codigoPolitica) {
        return switch (codigoPolitica) {
            case POLITICA_ALEATORIA -> "ALEATORIA";
            case POLITICA_PRIORIZADA -> "PRIORIZADA";
            default -> throw new IllegalArgumentException(
                    "Código de política inválido: " + codigoPolitica + ". Valores válidos: 0 (Aleatoria) o 1 (Priorizada)."
            );
        };
    }

    /**
     * create 7 threads for 5 exe seg SA..SE)
     * using S^3PR & Case 1,2 from assignmetn
     */
    private static List<Thread> crearHilosDeSegmentos(MonitorInterface monitor) {
        List<Thread> hilos = new ArrayList<>(7);

        // Segmento SA (previo al conflicto en P1): 1 hilo para {T0}
        hilos.add(new Thread(new SegmentWorker("SA", monitor, 0), "Hilo-SA-Admision"));

        // Segmento SB (rama tarjeta): 1 hilo para {T1, T2, T3}
        hilos.add(new Thread(new SegmentWorker("SB", monitor, 1, 2, 3), "Hilo-SB-Tarjeta"));

        // Segmento SC (rama alto riesgo): 1 hilo para {T4, T5}
        hilos.add(new Thread(new SegmentWorker("SC", monitor, 4, 5), "Hilo-SC-AltoRiesgo"));

        // Segmento SD (rama transferencia): 1 hilo para {T6, T7, T8}
        hilos.add(new Thread(new SegmentWorker("SD", monitor, 6, 7, 8), "Hilo-SD-Transferencia"));

        // Segmento SE (posterior al join en P9): 3 hilos para {T9} (cota máxima M(P9) = 3)
        for (int i = 1; i <= 3; i++) {
            hilos.add(new Thread(new SegmentWorker("SE-" + i, monitor, 9), "Hilo-SE-Liquidacion-" + i));
        }

        return hilos;
    }

    private static void verificarFinalizacionHilos(List<Thread> hilos) {
        long hilosVivos = hilos.stream().filter(Thread::isAlive).count();
        if (hilosVivos > 0) {
            throw new IllegalStateException("Error: quedaron " + hilosVivos + " hilos activos al terminar la ejecución.");
        }
    }

    private static void guardarResumenEjecucion(
            Path rutaResumen,
            int codigoPolitica,
            String nombrePolitica,
            List<Thread> hilos,
            double duracionSegundos,
            long totalDisparos,
            Path rutaLogDisparos
    ) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE EJECUCIÓN - RED DE PETRI PSP\n");
        sb.append("Política utilizada      : ").append(codigoPolitica).append(" (").append(nombrePolitica).append(")\n");
        sb.append("Cantidad total de hilos : ").append(hilos.size()).append("\n");
        for (Thread h : hilos) {
            sb.append("  - ").append(h.getName()).append(" -> estado final: ").append(h.getState()).append("\n");
        }
        sb.append(String.format("Tiempo total ejecución  : %.3f segundos%n", duracionSegundos));
        sb.append("Total disparos logueados: ").append(totalDisparos).append("\n");
        sb.append("Archivo log de disparos : ").append(rutaLogDisparos).append("\n");

        String resumen = sb.toString();
        System.out.println(resumen);
        Files.writeString(rutaResumen, resumen, StandardCharsets.UTF_8);
    }

    private static PetriNetConfig cargarConfiguracion(Path ruta, Integer politicaOverride) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(ruta)) {
            props.load(is);
        }

        int numPlazas = Integer.parseInt(props.getProperty("red.plazas", "10").trim());
        int numTransiciones = Integer.parseInt(props.getProperty("red.transiciones", "10").trim());

        int[][] matrixPre = new int[numPlazas][numTransiciones];
        int[][] matrixIncidencia = new int[numPlazas][numTransiciones];
        for (int i = 0; i < numPlazas; i++) {
            matrixPre[i] = parsearArrayInt(props.getProperty("red.pre." + i));
            matrixIncidencia[i] = parsearArrayInt(props.getProperty("red.incidencia." + i));
        }

        int[] marcadoInicial = parsearArrayInt(props.getProperty("red.marcado.inicial"));
        long[] tiempoMS = parsearArrayLong(props.getProperty("red.tiempos.ms"));
        int[] pesosPrio = parsearArrayInt(props.getProperty("politica.pesos"));

        int cantInv = Integer.parseInt(props.getProperty("invariantes.plaza.cantidad", "0").trim());
        int[][] invariantesP = new int[cantInv][numPlazas];
        int[] sumInvariantesP = new int[cantInv];
        for (int i = 0; i < cantInv; i++) {
            invariantesP[i] = parsearArrayInt(props.getProperty("invariantes.plaza." + i));
            sumInvariantesP[i] = Integer.parseInt(props.getProperty("invariantes.plaza." + i + ".suma").trim());
        }

        int transicionFinInvariantes = Integer.parseInt(props.getProperty("ejecucion.transicion.fin", "9").trim());
        int objetivoInvariantes = Integer.parseInt(props.getProperty("ejecucion.objetivo.invariantes", "200").trim());
        int tipoPolitica = (politicaOverride != null)
                ? politicaOverride
                : Integer.parseInt(props.getProperty("politica.tipo", "0").trim());

        return new PetriNetConfig(
                numPlazas,
                numTransiciones,
                matrixPre,
                matrixIncidencia,
                marcadoInicial,
                tiempoMS,
                pesosPrio,
                invariantesP,
                sumInvariantesP,
                transicionFinInvariantes,
                objetivoInvariantes,
                tipoPolitica
        );
    }

    private static int[] parsearArrayInt(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static long[] parsearArrayLong(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .toArray();
    }
}