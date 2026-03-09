package pcd.lab04.ex01_synchwithsem;

import java.util.concurrent.Semaphore;

import static java.rmi.server.LogStream.log;

public class Ponger extends ActiveComponent {

    Semaphore pingDoneEvent, pongDoneEvent;

    public Ponger(Semaphore wantPingEvent,  Semaphore wantPongEvent) {
        this.pingDoneEvent = wantPingEvent;
        this.pongDoneEvent = wantPongEvent;
    }
	
	public void run() {
		while (true) {
            try {
                this.pingDoneEvent.acquire();
                println("pong");
            } catch (InterruptedException ex) {
                log("interrupted..");
            }finally {
                this.pongDoneEvent.release();
            }
		}
	}
}