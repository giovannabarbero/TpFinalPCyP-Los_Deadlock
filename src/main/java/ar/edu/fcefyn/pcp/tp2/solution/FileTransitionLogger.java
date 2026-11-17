package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.TransitionLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileTransitionLogger implements TransitionLogger {
    private final BufferedWriter writer;
    private final Path archivoLog;
    private long totalDisparos = 0;

    public FileTransitionLogger(Path archivoLog) throws IOException {
        this.archivoLog = archivoLog;
        if (archivoLog.getParent() != null) {
            Files.createDirectories(archivoLog.getParent());
        }
        this.writer = Files.newBufferedWriter(
                archivoLog,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    @Override
    public synchronized void logTransition(int transition) {
        try {
            writer.write("T" + transition + " ");
            totalDisparos++;
        } catch (IOException e) {
            throw new RuntimeException("error al escribir disparo T" + transition + " en el log", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("error al cerrar el archivo de log: " + archivoLog, e);
        }
    }

    public synchronized long getTotalDisparos() {
        return totalDisparos;
    }

    public Path getArchivoLog() {
        return archivoLog;
    }
}
