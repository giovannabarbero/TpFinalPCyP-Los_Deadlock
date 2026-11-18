package ar.edu.fcefyn.pcp.tp2.app;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;
import ar.edu.fcefyn.pcp.tp2.api.PetriNetConfig;
import ar.edu.fcefyn.pcp.tp2.solution.PoliticaAleatoria;
 import ar.edu.fcefyn.pcp.tp2.solution.PoliticaPriorizada;
import ar.edu.fcefyn.pcp.tp2.api.Politica;
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
    public static final int POLITICA_PRIORIZADA = 1; // Encoding constants for ease of use

    public static void main(String[] args) throws Exception {
        // Relative pathing
        Path rutaConfig = Path.of("config", "config.properties"); // config file path loading
        Integer politicaPorConsola = (args.length > 0) ? Integer.parseInt(args[0].trim()) : null;// override by console code java -jar tp2.jar 0/1 prio

        PetriNetConfig config = cargarConfiguracion(rutaConfig, politicaPorConsola); // load config
        int codigoPolitica = config.tipoPolitica();
        String nombrePolitica = obtenerNombrePolitica(codigoPolitica);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); // time extract for unique log creation
        Path rutaLogDisparos = Path.of("resultados", "disparos_" + nombrePolitica + "_" + timestamp + ".log"); // path for firing log
        Path rutaResumen = Path.of("resultados", "resumen_" + nombrePolitica + "_" + timestamp + ".txt"); // path for final result summary

        FileTransitionLogger logger = new FileTransitionLogger(rutaLogDisparos); // create logger
        List<Thread> hilos;
        long inicioNs;
        long finNs;

        try {
            //MonitorInterface monitor = new Monitor(config, null, logger); old
            Politica politica = (codigoPolitica == POLITICA_PRIORIZADA)
                    ? new PoliticaPriorizada(config.pesosPrio())
                    : new PoliticaAleatoria();
            MonitorInterface monitor = new Monitor(config, politica, logger); // dependency inject. Monitor builds itself through its config, its policies, and its logger

            hilos = crearHilosDeSegmentos(monitor); // goto function - thread creation for each Petri net segment

            System.out.printf("Iniciando simulación PSP | Política: %d (%s) | Objetivo: %d invariantes | Hilos: %d%n",
                    codigoPolitica, nombrePolitica, config.objetivoInvariantes(), hilos.size());

            inicioNs = System.nanoTime(); // high precision time measure start

            for (Thread hilo : hilos) {
                hilo.start();
            } // start of each thread

            // join all threads for graceful stop
            for (Thread hilo : hilos) {
                hilo.join();
            }

            finNs = System.nanoTime(); // high precision time measure end
        } finally {
            logger.close(); // BufferedWriter will flush and close log even if something happens
        }

        verificarFinalizacionHilos(hilos); // goto function - checks if any thread is alive to alert

        double duracionSegundos = (finNs - inicioNs) / 1_000_000_000.0; // calculate runtime
        guardarResumenEjecucion(
                rutaResumen,
                codigoPolitica,
                nombrePolitica,
                hilos,
                duracionSegundos,
                logger.getTotalDisparos(),
                rutaLogDisparos
        ); // shows on console and saves the summary
    }

    private static String obtenerNombrePolitica(int codigoPolitica) {
        return switch (codigoPolitica) {
            case POLITICA_ALEATORIA -> "ALEATORIA";
            case POLITICA_PRIORIZADA -> "PRIORIZADA";
            default -> throw new IllegalArgumentException(
                    "Código de política inválido: " + codigoPolitica + ". Valores válidos: 0 (Aleatoria) o 1 (Priorizada)."
            );
        };
    } // translates codification to string

    /**
     * create 7 threads for 5 exe seg SA..SE)
     * using S^3PR & Case 1,2 from assignmetn
     */
    private static List<Thread> crearHilosDeSegmentos(MonitorInterface monitor) {
        List<Thread> hilos = new ArrayList<>(7);

        // Segment SA (previous to conflicto in P1): 1 thread for {T0}
        hilos.add(new Thread(new SegmentWorker("SA", monitor, 0), "Hilo-SA-Admision")); // this thread holds no decision over which flow has to goto next, that is managed by policy

        // Segment SB (branch cards): 1 hilo for {T1, T2, T3}
        hilos.add(new Thread(new SegmentWorker("SB", monitor, 1, 2, 3), "Hilo-SB-Tarjeta"));

        // Segment SC (branch high risk): 1 thread for {T4, T5}
        hilos.add(new Thread(new SegmentWorker("SC", monitor, 4, 5), "Hilo-SC-AltoRiesgo"));

        // Segment SD (branch transfer): 1 thread for {T6, T7, T8}
        hilos.add(new Thread(new SegmentWorker("SD", monitor, 6, 7, 8), "Hilo-SD-Transferencia"));

        // Segment SE (posterior to join in P9): 3 threads for {T9} (máx amount M(P9) = 3)
        for (int i = 1; i <= 3; i++) {
            hilos.add(new Thread(new SegmentWorker("SE-" + i, monitor, 9), "Hilo-SE-Liquidacion-" + i)); // as the inicial marks are 3, then 3 transactions can be ready to liquidate in P9 at the same time, 3 threads allows for parallel liquidation
        }

        return hilos;
    }

    private static void verificarFinalizacionHilos(List<Thread> hilos) {
        long hilosVivos = hilos.stream().filter(Thread::isAlive).count(); // check if any threads are left alive
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
    } // creates summary file structure

    private static PetriNetConfig cargarConfiguracion(Path ruta, Integer politicaOverride) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(ruta)) { // imput stream makes a byte flow
            props.load(is);
        } // load properties file

        int numPlazas = Integer.parseInt(props.getProperty("red.plazas", "10").trim());
        int numTransiciones = Integer.parseInt(props.getProperty("red.transiciones", "10").trim());
        // parse makes csv into java primitives
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
        ); // packing inmutable record
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