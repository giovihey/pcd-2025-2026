package pcd.lab05.monitors;

import java.util.concurrent.locks.*;

public class SynchCell2 {

	private int value;
	private boolean available;
	private Lock lock;
	private Condition isAvail;

	public SynchCell2(){
		available = false;
		lock = new ReentrantLock();
		isAvail = lock.newCondition();
	}

	public void set(int v){
		try {
			lock.lock();
			value = v;
			available = true;
			isAvail.signalAll();  
		} finally {
			lock.unlock();
		}
	}
	
	public int get() {
		try {
			lock.lock();
			while (!available){
				try {
					isAvail.await();
				} catch (InterruptedException ex){}
			} 
			return value;
		} finally {
			lock.unlock();
		}
	}
}

