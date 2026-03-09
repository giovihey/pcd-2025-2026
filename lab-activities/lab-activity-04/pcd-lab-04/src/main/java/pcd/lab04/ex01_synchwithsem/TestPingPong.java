package pcd.lab04.ex01_synchwithsem;

import java.util.concurrent.Semaphore;

/**
 * Unsynchronized version
 * 
 * @TODO make it sync 
 * @author aricci
 *
 */
public class TestPingPong {
	public static void main(String[] args) {
        Semaphore pingDoneEvent = new Semaphore(0);
        Semaphore pongDoneEvent = new Semaphore(0);

        new Pinger(pongDoneEvent, pingDoneEvent).start();
		new Ponger(pingDoneEvent, pongDoneEvent).start();

        pongDoneEvent.release();
	}

}
