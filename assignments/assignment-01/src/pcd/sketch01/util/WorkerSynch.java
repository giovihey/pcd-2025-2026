package pcd.sketch01.util;

public class WorkerSynch {
    private int generation = 0;

    public synchronized void waitForStart() throws InterruptedException {
        int myGen = generation;
        while (generation == myGen) {
            wait();
        }
    }

    public synchronized void signal() {
        generation++;
        notifyAll();
    }
}
