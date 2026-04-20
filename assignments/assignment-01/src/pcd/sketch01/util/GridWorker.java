package pcd.sketch01.util;

import pcd.sketch01.model.Ball;
import pcd.sketch01.model.Board;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GridWorker extends Thread {
    private final BarrierSynch barrier;
    private final WorkerSynch startSynch;
    private final ConcurrentHashMap<Integer, List<Ball>> grid;
    private final int startRow, endRow;
    private final Board board;

    public GridWorker(BarrierSynch barrier, WorkerSynch startSynch,
                      ConcurrentHashMap<Integer, List<Ball>> grid,
                      int startRow, int endRow, Board board) {
        this.barrier = barrier;
        this.startSynch = startSynch;
        this.grid = grid;
        this.startRow = startRow;
        this.endRow = endRow;
        this.board = board;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                startSynch.waitForStart();
                resolveGridRows();
                barrier.hitAndWaitAll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void resolveGridRows() {
        int cols = board.getCOLS();
        int rows = board.getROWS();

        // Neighbor offsets: only 4 directions to avoid duplicate pairs
        // (self done inline, only check right/down/diagonals forward)
        int[][] neighborOffsets = {
                {1, 0},   // right
                {-1, 1},  // bottom-left
                {0, 1},   // bottom
                {1, 1}    // bottom-right
        };

        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int key = col + row * cols;
                List<Ball> cell = grid.get(key);
                if (cell == null || cell.isEmpty()) continue;

                // 1. Intra-cell collisions (same cell)
                int size = cell.size();
                for (int i = 0; i < size - 1; i++) {
                    Ball a = cell.get(i);
                    for (int j = i + 1; j < size; j++) {
                        Ball b = cell.get(j);
                        if (overlaps(a, b)) Ball.resolveCollision(a, b, board);
                    }
                }

                // 2. Inter-cell: only 4 forward neighbors
                for (int[] offset : neighborOffsets) {
                    int nx = col + offset[0];
                    int ny = row + offset[1];
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                    if (ny < startRow || ny >= endRow) continue;

                    int nkey = nx + ny * cols;
                    List<Ball> neigh = grid.get(nkey);
                    if (neigh == null || neigh.isEmpty()) continue;

                    for (Ball a : cell) {
                        for (Ball b : neigh) {
                            if (overlaps(a, b)) Ball.resolveCollision(a, b, board);
                        }
                    }
                }
            }
        }
    }

    // Broad-phase: no sqrt, just squared distance check
    private boolean overlaps(Ball a, Ball b) {
        double dx = a.getPos().x() - b.getPos().x();
        double dy = a.getPos().y() - b.getPos().y();
        double r = a.getRadius() + b.getRadius();
        return dx * dx + dy * dy < r * r;
    }
}