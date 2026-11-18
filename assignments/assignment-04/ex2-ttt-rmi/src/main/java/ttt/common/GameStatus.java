package ttt.common;

public enum GameStatus {
    WAITING_FOR_OPPONENT, IN_PROGRESS, X_WON, O_WON, DRAW,
    /** A player left or became unreachable before the end. */
    ABORTED;

    public boolean isFinished() {
        return this != WAITING_FOR_OPPONENT && this != IN_PROGRESS;
    }
}
