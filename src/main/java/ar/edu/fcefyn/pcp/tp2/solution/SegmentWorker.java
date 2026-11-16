package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;

public class SegmentWorker implements Runnable{
    private final String nameSegmento;
    private final MonitorInterface monitor;
    private final int[] secuenciaFire;

    public SegmentWorker (String nombreseg, MonitorInterface mon, int... seqF) {
        this.nameSegmento = nombreseg;
        this.monitor = mon;
        this.secuenciaFire = seqF;
    }

    @Override
    public void run(){
        while (!Thread.currentThread().isInterrupted()) {
            for (int transicion : secuenciaFire) {
                boolean continuar = monitor.fireTransition(transicion);
                if (!continuar) {
                    return;
                }
            }
        }
    }

    public String getNameSegmento() {
        return nameSegmento;
    }
}
