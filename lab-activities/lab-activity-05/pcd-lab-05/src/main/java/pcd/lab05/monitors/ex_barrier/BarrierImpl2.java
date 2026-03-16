package pcd.lab05.monitors.ex_barrier;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BarrierImpl2 implements Barrier {

    private int nParticipants, nArrived;
    private Lock lock;
    private Condition allArrived;

    public BarrierImpl2(int nParticipants) {
        this.nParticipants = nParticipants;
        this.lock = new ReentrantLock();
        this.nArrived = 0;
        this.allArrived = lock.newCondition();
    }

    @Override
    public void hitAndWaitAll() throws InterruptedException {
        try {
            lock.lock();
            nArrived++;
            if  (nArrived < nParticipants) {
                while (nArrived < nParticipants ) {
                    allArrived.await();
                }
            } else {
                allArrived.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
