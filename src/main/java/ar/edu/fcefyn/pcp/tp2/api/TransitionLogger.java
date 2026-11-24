package ar.edu.fcefyn.pcp.tp2.api;

public interface TransitionLogger {
    // Registrar cada disparo dentro monitor deacoplado del logger
    void logTransition(int transition);
    // Registrar cada decision de la politica (solo cuando hay 2 o mas candidatas)
    void logDecision(boolean[] candidatas, int elegida);
    void close();
}