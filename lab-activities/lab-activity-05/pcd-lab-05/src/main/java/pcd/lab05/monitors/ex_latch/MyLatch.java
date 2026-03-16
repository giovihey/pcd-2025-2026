package pcd.lab05.monitors.ex_latch;

public class MyLatch implements Latch {

    private int nCountDowns;
    private int nCounts;

    public MyLatch(int nCountDowns) {
        this.nCountDowns = nCountDowns;
        this.nCounts = 0;
    }

    @Override
    public synchronized void countDown() throws InterruptedException {
        while (nCounts < nCountDowns) {
            wait();
        }

    }

    @Override
    public synchronized void await() throws InterruptedException {
        nCounts++;
        if (nCounts == nCountDowns) {
            notifyAll();
        }
    }
}
