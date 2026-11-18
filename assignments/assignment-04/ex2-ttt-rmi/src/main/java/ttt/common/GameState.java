package ttt.common;

import java.io.Serializable;

/**
 * Immutable snapshot of a game, with all the rules of Tic-Tac-Toe.
 * Being immutable it can be shared between threads and sent over RMI without any synchronization:
 * every change returns a new state.
 */
public final class GameState implements Serializable {

    public static final int SIZE = 3;
    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},   // rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},   // columns
        {0, 4, 8}, {2, 4, 6}               // diagonals
    };

    private final Symbol[] cells;          // row-major, null = empty
    private final Symbol turn;
    private final GameStatus status;

    private GameState(Symbol[] cells, Symbol turn, GameStatus status) {
        this.cells = cells;
        this.turn = turn;
        this.status = status;
    }

    public static GameState waitingForOpponent() {
        return new GameState(new Symbol[SIZE * SIZE], Symbol.X, GameStatus.WAITING_FOR_OPPONENT);
    }

    /** The opponent joined: the game starts. */
    public GameState started() {
        if (status != GameStatus.WAITING_FOR_OPPONENT) {
            throw new IllegalStateException("already started");
        }
        return new GameState(cells, Symbol.X, GameStatus.IN_PROGRESS);
    }

    /** A player left: the game is over. */
    public GameState aborted() {
        return isFinished() ? this : new GameState(cells, turn, GameStatus.ABORTED);
    }

    /** Returns the state after the move, or throws GameException if the move is not allowed. */
    public GameState applyMove(Symbol player, int row, int col) throws GameException {
        if (status == GameStatus.WAITING_FOR_OPPONENT) {
            throw new GameException("The game is waiting for an opponent");
        }
        if (isFinished()) {
            throw new GameException("The game is over");
        }
        if (player != turn) {
            throw new GameException("Not your turn");
        }
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new GameException("Cell out of the board");
        }
        if (cells[row * SIZE + col] != null) {
            throw new GameException("Cell already taken");
        }
        Symbol[] next = cells.clone();
        next[row * SIZE + col] = player;

        GameStatus nextStatus = GameStatus.IN_PROGRESS;
        if (hasWon(next, player)) {
            nextStatus = (player == Symbol.X) ? GameStatus.X_WON : GameStatus.O_WON;
        } else if (isFull(next)) {
            nextStatus = GameStatus.DRAW;
        }
        return new GameState(next, player.opponent(), nextStatus);
    }

    /** The mark in the cell, or null if empty. */
    public Symbol cellAt(int row, int col) {
        return cells[row * SIZE + col];
    }

    public Symbol turn() {
        return turn;
    }

    public GameStatus status() {
        return status;
    }

    public boolean isFinished() {
        return status.isFinished();
    }

    private static boolean hasWon(Symbol[] c, Symbol s) {
        for (int[] l : LINES) {
            if (c[l[0]] == s && c[l[1]] == s && c[l[2]] == s) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(Symbol[] c) {
        for (Symbol s : c) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Symbol s = cells[r * SIZE + c];
                sb.append(s == null ? '.' : s.name()).append(c < SIZE - 1 ? " " : "");
            }
            sb.append(r < SIZE - 1 ? "\n" : "");
        }
        return sb.toString();
    }
}
