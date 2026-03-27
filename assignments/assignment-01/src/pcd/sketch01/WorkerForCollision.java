package pcd.sketch01;

import java.util.List;

public class WorkerForCollision extends Thread {
    private final BarrierSynch barrier;
    private final WorkerSynch startSynch;
    private final List<Ball> balls;
    private final int fromIndex; // start of this worker's range
    private final int toIndex;   // end of this worker's range
    private final Board board;
    private volatile boolean alive = true;

    public WorkerForCollision(BarrierSynch barrier, WorkerSynch startSynch,
                              List<Ball> balls, int fromIndex, int toIndex, Board board) {
        this.barrier = barrier;
        this.startSynch = startSynch;
        this.balls = balls;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.board = board;
    }

    @Override
    public void run() {
        while (alive) {
            try {
                startSynch.waitForStart();
                System.out.println("startSynch.waitForStart()");// sleep until simulation signals
                resolvePartition();                // do the work
                barrier.hitAndWaitAll();
                System.out.println("barrier.hitAndWaitAll();");// meet simulation thread
            } catch (InterruptedException e) {
                alive = false;
            } catch (Exception e) {
                e.printStackTrace(); // <-- add this
                alive = false;       // <-- worker dies here, barrier never reached
            }
        }
    }

    private void resolvePartition() {
        System.out.println("start Synch.resolvePartition()");
        for (int i = fromIndex; i < toIndex - 1; i++)
            for (int j = i + 1; j < toIndex; j++)
                Ball.resolveCollision(balls.get(i), balls.get(j), board);
    }

    public void shutdown() { alive = false; }
}
