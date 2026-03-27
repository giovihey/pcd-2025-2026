package pcd.sketch01;

import java.util.*;

public class Board {

    private List<Ball> balls;
    private Ball playerBall;
    private Ball botBall;
    private List<Hole> holes;
    private Boundary bounds;

    private BarrierSynch barrier;
    private List<WorkerSynch> synchWorkers;
    private List<WorkerForCollision>  workers;

    private int playerScore = 0;
    private int botScore = 0;

    public Board(){} 
    
    public void init(BoardConf conf) {
    	this.balls = conf.getSmallBalls();
    	this.playerBall = conf.getPlayerBall();
        this.botBall = conf.getBotBall();
        this.holes = conf.getHoles();
    	this.bounds = conf.getBoardBoundary();
        int nWorkers = Math.min(
                Runtime.getRuntime().availableProcessors(),
                balls.size()
        );
        this.barrier = new BarrierSynch(nWorkers + 1);

        this.synchWorkers = new  ArrayList<>();
        this.workers = new  ArrayList<>();

        int chunkSize = balls.size() / nWorkers;
        for (int i = 0; i < nWorkers; i++) {
            int from = i * chunkSize;
            int to = (i == nWorkers - 1) ? balls.size() : from + chunkSize;
            // last worker takes the remainder

            WorkerSynch startSynch = new WorkerSynch();
            WorkerForCollision worker = new WorkerForCollision(barrier, startSynch, balls, from, to, this);
            synchWorkers.add(startSynch);
            workers.add(worker);
            worker.start();
        }

        System.out.println("nWorkers: " + nWorkers + ", barrier participants: " + (nWorkers + 1));
        System.out.println("balls.size(): " + balls.size());
    }

    public void updateState(long dt) {
        playerBall.updateState(dt, this);
        botBall.updateState(dt, this);

        for (var b : balls) b.updateState(dt, this);

        // signal all workers and wait for them at the barrier
        for (var s : synchWorkers) s.signal();
        try {
            barrier.hitAndWaitAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (var b : balls) Ball.resolveCollision(playerBall, b, this);
        for (var b : balls) Ball.resolveCollision(botBall, b, this);

        Ball.resolveCollision(playerBall, botBall, this);

        // Score and remove balls that fall into holes
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            for (Hole hole : holes) {
                double dx   = ball.getPos().x() - hole.pos().x();
                double dy   = ball.getPos().y() - hole.pos().y();
                double dist = Math.hypot(dx, dy);
                if (dist < hole.radius()) {
                    if      (ball.getLastHitter() == Hitter.PLAYER) incrementPlayerScore();
                    else if (ball.getLastHitter() == Hitter.BOT)    incrementBotScore();
                    it.remove();
                    break;
                }
            }
        }
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public Ball getPlayerBall() {
        return playerBall;
    }

    public Ball getBotBall() {
        return botBall;
    }

    public List<Hole> getHoles() {
        return holes;
    }

    public Boundary getBounds() {
        return bounds;
    }

    public int getPlayerScore() { return playerScore; }

    public int getBotScore() { return botScore; }

    public void incrementPlayerScore() { playerScore++; }

    public void incrementBotScore() { botScore++; }
}
