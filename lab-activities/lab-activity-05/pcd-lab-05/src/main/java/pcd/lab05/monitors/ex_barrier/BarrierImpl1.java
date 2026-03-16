package pcd.lab05.monitors.ex_barrier;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BarrierImpl1 implements Barrier {

    private int nParticipants, nArrived;

    public BarrierImpl1(int nParticipants) {
        this.nParticipants = nParticipants;
        this.nArrived = 0;
    }

    @Override
    public synchronized void hitAndWaitAll() throws InterruptedException {
        nArrived++;
        if  (nArrived < nParticipants) {
            while (nParticipants > nArrived) {
                wait();
            }
        } else {
            notifyAll();
        }
    }
}