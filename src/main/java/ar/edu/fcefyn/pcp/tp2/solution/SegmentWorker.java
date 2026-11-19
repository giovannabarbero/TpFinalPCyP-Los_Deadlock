package ar.edu.fcefyn.pcp.tp2.solution;

import ar.edu.fcefyn.pcp.tp2.api.MonitorInterface;

public class SegmentWorker implements Runnable{
    private final String nameSegmento;
    private final MonitorInterface monitor;
    private final int[] secuenciaFire;

    // Initialization of worker thread instance for segment exe
    public SegmentWorker (String nombreseg, MonitorInterface mon, int... seqF) {
        this.nameSegmento = nombreseg; // description of segment
        this.monitor = mon; //  reference to shared monitor decoupled from concrete class
        this.secuenciaFire = seqF; // varags comma separated sequence of transition ids to fire in run loop
    }

    @Override
    public void run(){
        while (!Thread.currentThread().isInterrupted()) {
            for (int transicion : secuenciaFire) {
                boolean continuar = monitor.fireTransition(transicion); // only public concurrency method
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
