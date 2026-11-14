package ar.edu.fcefyn.pcp.tp2.api;

public interface TransitionLogger {
    // Registrar cada disparo dentro monitor deacoplado del logger
    void logTransition(int transition);
    void close();
}
