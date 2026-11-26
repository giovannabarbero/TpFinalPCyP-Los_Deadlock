package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.TransitionLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileTransitionLogger implements TransitionLogger {
    private final BufferedWriter writerDisparos;
    private final BufferedWriter writerDecisiones;
    private final Path archivoLogDisparos;
    private final Path archivoLogDecisiones;
    private long totalDisparos = 0;

    public FileTransitionLogger(Path archivoLogDisparos, Path archivoLogDecisiones) throws IOException {
        this.archivoLogDisparos = archivoLogDisparos;
        this.archivoLogDecisiones = archivoLogDecisiones;

        if (archivoLogDisparos.getParent() != null) {
            Files.createDirectories(archivoLogDisparos.getParent());
        }
        if (archivoLogDecisiones.getParent() != null) {
            Files.createDirectories(archivoLogDecisiones.getParent());
        }

        this.writerDisparos = Files.newBufferedWriter(
                archivoLogDisparos,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        this.writerDecisiones = Files.newBufferedWriter(
                archivoLogDecisiones,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    @Override
    public synchronized void logTransition(int transition) {
        try {
            writerDisparos.write("T" + transition + " ");
            totalDisparos++;
        } catch (IOException e) {
            throw new RuntimeException("error al escribir disparo T" + transition + " en el log", e);
        }
    }

    @Override
    public synchronized void logDecision(boolean[] candidatas, int elegida) {
        if (candidatas == null) {
            return;
        }
        List<String> activas = new ArrayList<>();
        for (int i = 0; i < candidatas.length; i++) {
            if (candidatas[i]) {
                activas.add("T" + i);
            }
        }
        if (activas.size() < 2) {
            return;
        }

        String textoElegida = (elegida >= 0) ? "T" + elegida : "NINGUNA";
        String linea = "candidatas=" + String.join(", ", activas) + " elegida=" + textoElegida + System.lineSeparator();

        try {
            writerDecisiones.write(linea);
        } catch (IOException e) {
            throw new RuntimeException("error al escribir decisión en el log", e);
        }
    }

    @Override
    public synchronized void close() {
        IOException error = null;
        try {
            writerDisparos.flush();
        } catch (IOException e) {
            error = e;
        }
        try {
            writerDisparos.close();
        } catch (IOException e) {
            if (error == null) error = e;
        }

        try {
            writerDecisiones.flush();
        } catch (IOException e) {
            if (error == null) error = e;
        }
        try {
            writerDecisiones.close();
        } catch (IOException e) {
            if (error == null) error = e;
        }

        if (error != null) {
            throw new RuntimeException("error al cerrar los archivos de log", error);
        }
    }

    public synchronized long getTotalDisparos() {return totalDisparos;}

    public Path getArchivoLogDisparos() {return archivoLogDisparos;}

    public Path getArchivoLogDecisiones() {return archivoLogDecisiones;}
}
