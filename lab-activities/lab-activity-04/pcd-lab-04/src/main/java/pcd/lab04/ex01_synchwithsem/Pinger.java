package pcd.lab04.ex01_synchwithsem;

import java.util.concurrent.Semaphore;

import static java.rmi.server.LogStream.log;

public class Pinger extends ActiveComponent {

    Semaphore pongDoneEvent, pingDoneEvent;

	public Pinger(Semaphore pongDoneEvent, Semaphore pingDoneEvent) {
        this.pongDoneEvent = pongDoneEvent;
        this.pingDoneEvent = pingDoneEvent;
	}	
	
	public void run() {
		while (true) {
            try {
                this.pongDoneEvent.acquire();
                println("ping");
            } catch (InterruptedException ex) {
                log("interrupted..");
            }finally {
                this.pingDoneEvent.release();
            }
		}
	}
}