package pcd.sketch01;

public class WorkerSynch {
    private boolean startSignal = false;

    public synchronized void waitForStart() throws InterruptedException {
        while (!startSignal) {
            wait();
        }
        startSignal = false; // consume the signal
    }

    public synchronized void signal() {
        startSignal = true;
        notifyAll();
    }
}
