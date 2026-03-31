package pcd.sketch01.util;

public class BarrierSynch {
    private final int nParticipants;
    private int nArrived;
    private int generation;

    public BarrierSynch(int nParticipants) {
        this.nParticipants = nParticipants;
        this.nArrived = 0;
        this.generation = 0;
    }

    public synchronized void hitAndWaitAll() throws InterruptedException {
        int myGeneration = generation;
        nArrived++;
        //System.out.println("nArrived: " + nArrived);
        if  (nArrived < nParticipants) {
            while (generation == myGeneration) {
               // System.out.println("waiting");
            // ← wait until generation changes
                wait();
            }
        } else {
            nArrived = 0;
            generation++;
            notifyAll();
            //System.out.println("Notify all barrier hitAndWaitAll");
        }
    }
}
