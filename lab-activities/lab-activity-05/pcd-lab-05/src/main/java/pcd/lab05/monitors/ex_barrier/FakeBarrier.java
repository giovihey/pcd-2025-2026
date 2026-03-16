package pcd.lab05.monitors.ex_barrier;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Barrier - to be implemented
 */
public class FakeBarrier implements Barrier {

    int syncParticipants;
    Lock lock;
    boolean lastParticipant = false;
    private Condition isAvail;

	public FakeBarrier(int nParticipants) {
        this.syncParticipants = nParticipants;
        lock = new ReentrantLock();
        isAvail = lock.newCondition();
    }
	
	@Override
	public void hitAndWaitAll() throws InterruptedException {
        try {
            lock.lock();
            syncParticipants--;
            isAvail.signalAll();
        } catch (Exception e){}
        finally {
            lock.unlock();
        }
        try {
            lock.lock();
            while (syncParticipants > 0){
                try {
                    isAvail.await();
                } catch (InterruptedException ex){}
            }
        } finally {
            lock.unlock();
        }
    }
}

