package ttt.server;

import ttt.common.GameException;
import ttt.common.GameState;
import ttt.common.GameStatus;
import ttt.common.PlayerListener;
import ttt.common.Symbol;

/**
 * One game, designed as a MONITOR: all the state is private and every method is synchronized,
 * so join / move / abort on the same game are atomic with respect to each other.
 * Different games are different monitors, so they never block each other.
 * No remote call is made here: callbacks are done by GameServerImpl after the monitor is released.
 */
final class Game {
    private GameState state = GameState.waitingForOpponent();
    private final PlayerListener creator;
    private PlayerListener opponent;      // null until somebody joins

    Game(PlayerListener creator) {
        this.creator = creator;
    }

    synchronized boolean isOpen() {
        return state.status() == GameStatus.WAITING_FOR_OPPONENT;
    }

    /** Join an open game: of many concurrent joiners only one succeeds. */
    synchronized GameState join(PlayerListener listener) throws GameException {
        if (!isOpen()) {
            throw new GameException("The game is not open");
        }
        opponent = listener;
        state = state.started();
        return state;
    }

    /** Validates and applies the move atomically. */
    synchronized GameState move(Symbol player, int row, int col) throws GameException {
        state = state.applyMove(player, row, col);
        return state;
    }

    /** @return true if this call ended the game, false if it was already over. */
    synchronized boolean abort() {
        if (state.isFinished()) {
            return false;
        }
        state = state.aborted();
        return true;
    }

    synchronized GameState state() {
        return state;
    }

    /** The callback of a player, null if the opponent has not joined yet. */
    synchronized PlayerListener listenerOf(Symbol player) {
        return player == Symbol.X ? creator : opponent;
    }
}
