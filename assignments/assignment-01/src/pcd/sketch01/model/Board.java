package pcd.sketch01.model;

import pcd.sketch01.util.BarrierSynch;
import pcd.sketch01.util.WorkerSynch;
import pcd.sketch01.util.GridWorker;  // NEW: Add this class (code below)

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class Board {

    private List<Ball> balls;
    private Ball playerBall;
    private Ball botBall;
    private List<Hole> holes;
    private Boundary bounds;

    // GRID FIELDS (NEW)
    private static final double CELL_SIZE = 25.0;  // Tune ~4x avg ball radius
    private int COLS, ROWS;
    private final ConcurrentHashMap<Integer, List<Ball>> grid = new ConcurrentHashMap<>();

    // EXISTING CONCURRENCY (reused)
    private BarrierSynch barrier;
    private List<WorkerSynch> synchWorkers = new ArrayList<>();
    private List<Thread> workers = new ArrayList<>();  // Now GridWorkers

    private int playerScore = 0;
    private int botScore = 0;

    public Board() {}

    public void init(BoardConf conf) {
        this.balls = new ArrayList<>(conf.getSmallBalls());
        this.playerBall = conf.getPlayerBall();
        this.botBall = conf.getBotBall();
        this.holes = new ArrayList<>(conf.getHoles());
        this.bounds = conf.getBoardBoundary();

        // GRID SETUP
        this.COLS = (int) (bounds.x1() / CELL_SIZE) + 1;
        this.ROWS = (int) (bounds.y1() / CELL_SIZE) + 1;

        // WORKERS: One per row-chunk (platform threads)
        int nWorkers = Math.min(Runtime.getRuntime().availableProcessors(), ROWS);
        this.barrier = new BarrierSynch(nWorkers + 1);
        int rowChunk = ROWS / nWorkers;
        for (int i = 0; i < nWorkers; i++) {
            int fromRow = i * rowChunk;
            int toRow = (i == nWorkers - 1) ? ROWS : fromRow + rowChunk;
            WorkerSynch startSynch = new WorkerSynch();
            GridWorker worker = new GridWorker(barrier, startSynch, grid, fromRow, toRow, this);
            synchWorkers.add(startSynch);
            workers.add(worker);
            worker.start();
        }
    }

    public void updateState(long dt) {
        // Update positions/friction/boundaries (serial, fast)
        playerBall.updateState(dt, this);
        botBall.updateState(dt, this);
        for (var b : balls) {
            b.updateState(dt, this);
        }

        // PARALLEL GRID COLLISIONS (NEW)
        buildGrid();
        for (var s : synchWorkers) {
            s.signal();
        }
        try {
            barrier.hitAndWaitAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Sequential polish (low cost)
        for (var b : balls) {
            Ball.resolveCollision(playerBall, b, this);
            Ball.resolveCollision(botBall, b, this);
        }
        Ball.resolveCollision(playerBall, botBall, this);

        // Holes/scoring (serial)
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            for (Hole hole : holes) {
                double dx = ball.getPos().x() - hole.pos().x();
                double dy = ball.getPos().y() - hole.pos().y();
                double dist = Math.hypot(dx, dy);
                if (dist < hole.radius()) {
                    if (ball.getLastHitter() == Hitter.PLAYER) {
                        incrementPlayerScore();
                    } else if (ball.getLastHitter() == Hitter.BOT) {
                        incrementBotScore();
                    }
                    it.remove();
                    break;
                }
            }
        }
    }

    // NEW: O(n) grid build (main thread)
    private void buildGrid() {
        grid.clear();
        for (Ball b : balls) {
            int cx = Math.floorDiv((int) b.getPos().x(), (int) CELL_SIZE);
            int cy = Math.floorDiv((int) b.getPos().y(), (int) CELL_SIZE);
            cx = (cx % COLS + COLS) % COLS;  // Wrap toroidal if needed
            int key = cx + cy * COLS;
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
        }
    }

    // GETTERS FOR GRIDWORKER
    public int getCOLS() { return COLS; }
    public int getROWS() { return ROWS; }

    // Shutdown (call from GameController)
    public void shutdownWorkers() {
        for (Thread w : workers) {
            ((GridWorker) w).shutdown();
        }
    }

    // Unchanged getters
    public List<Ball> getBalls() { return balls; }
    public Ball getPlayerBall() { return playerBall; }
    public Ball getBotBall() { return botBall; }
    public List<Hole> getHoles() { return holes; }
    public Boundary getBounds() { return bounds; }
    public int getPlayerScore() { return playerScore; }
    public int getBotScore() { return botScore; }
    public void incrementPlayerScore() { playerScore++; }
    public void incrementBotScore() { botScore++; }
}