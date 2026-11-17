package ar.edu.fcefyn.pcp.tp2.utilities;

public class ResultadoValidacion {
    private final boolean valido;
    private final int totalInvariantes;
    private final int conteoCamino1; // T0 -> T1 -> T2 -> T3 -> T9
    private final int conteoCamino2; // T0 -> T4 -> T5 -> T9
    private final int conteoCamino3; // T0 -> T6 -> T7 -> T8 -> T9
    private final String remanente;
    private final String mensaje;

    public ResultadoValidacion(boolean valido, int totalInvariantes,
                               int conteoCamino1, int conteoCamino2, int conteoCamino3,
                               String remanente, String mensaje) {
        this.valido = valido;
        this.totalInvariantes = totalInvariantes;
        this.conteoCamino1 = conteoCamino1;
        this.conteoCamino2 = conteoCamino2;
        this.conteoCamino3 = conteoCamino3;
        this.remanente = remanente;
        this.mensaje = mensaje;
    }

    public boolean esValido() { return valido; }
    public int getCantidadInvariantes() { return totalInvariantes; }
    public int getConteoCamino1() { return conteoCamino1; }
    public int getConteoCamino2() { return conteoCamino2; }
    public int getConteoCamino3() { return conteoCamino3; }
    public String getRemanente() { return remanente; }
    public String getMensaje() { return mensaje; }
}
