package pcd.lab05.monitors.ex_latch;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LatchImpl2 implements Latch {

    private int nCountDowns;
    private int nCounts;
    private Lock lock;
    private Condition allCountDone;

    public LatchImpl2(int nCountDowns) {
        this.nCountDowns = nCountDowns;
        this.nCounts = 0;
        this.lock = new ReentrantLock();
        this.allCountDone = lock.newCondition();
    }

    @Override
    public synchronized void countDown() throws InterruptedException {
        try {
            lock.lock();
            while (nCounts < nCountDowns) {
                allCountDone.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized void await() throws InterruptedException {
        try {
            lock.lock();
            nCounts++;
            while (nCounts < nCountDowns) {
                allCountDone.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
