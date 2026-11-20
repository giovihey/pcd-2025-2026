package ttt.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GameStateTest {

    /** Plays the moves (row, col pairs) alternately, starting with X, on a started game. */
    private static GameState play(int... moves) throws GameException {
        GameState s = GameState.waitingForOpponent().started();
        Symbol player = Symbol.X;
        for (int i = 0; i < moves.length; i += 2) {
            s = s.applyMove(player, moves[i], moves[i + 1]);
            player = player.opponent();
        }
        return s;
    }

    @Test
    void newGameWaitsForAnOpponent() {
        GameState s = GameState.waitingForOpponent();
        assertEquals(GameStatus.WAITING_FOR_OPPONENT, s.status());
        assertNull(s.cellAt(1, 1));
    }

    @Test
    void startedGameGivesTheFirstTurnToX() {
        GameState s = GameState.waitingForOpponent().started();
        assertEquals(GameStatus.IN_PROGRESS, s.status());
        assertEquals(Symbol.X, s.turn());
    }

    @Test
    void turnsAlternate() throws GameException {
        GameState s = play(0, 0);
        assertEquals(Symbol.X, s.cellAt(0, 0));
        assertEquals(Symbol.O, s.turn());
    }

    @Test
    void stateIsImmutable() throws GameException {
        GameState before = GameState.waitingForOpponent().started();
        before.applyMove(Symbol.X, 0, 0);
        assertNull(before.cellAt(0, 0));
    }

    @Test
    void illegalMovesAreRejected() throws GameException {
        GameState s = play(1, 1);
        assertThrows(GameException.class, () -> s.applyMove(Symbol.X, 0, 0));   // not X's turn
        assertThrows(GameException.class, () -> s.applyMove(Symbol.O, 1, 1));   // cell taken
        assertThrows(GameException.class, () -> s.applyMove(Symbol.O, 3, 0));   // out of the board
        assertThrows(GameException.class,
                () -> GameState.waitingForOpponent().applyMove(Symbol.X, 0, 0)); // not started
    }

    @Test
    void rowWinEndsTheGame() throws GameException {
        GameState s = play(0, 0, 1, 0, 0, 1, 1, 1, 0, 2);
        assertEquals(GameStatus.X_WON, s.status());
        assertThrows(GameException.class, () -> s.applyMove(Symbol.O, 2, 2));
    }

    @Test
    void columnAndDiagonalWins() throws GameException {
        assertEquals(GameStatus.O_WON, play(0, 0, 0, 1, 1, 0, 1, 1, 2, 2, 2, 1).status());
        assertEquals(GameStatus.X_WON, play(0, 0, 0, 1, 1, 1, 0, 2, 2, 2).status());
    }

    @Test
    void fullBoardWithoutLineIsADraw() throws GameException {
        // X O X / X O O / O X X
        GameState s = play(0, 0, 0, 1, 0, 2, 1, 1, 1, 0, 1, 2, 2, 1, 2, 0, 2, 2);
        assertEquals(GameStatus.DRAW, s.status());
    }

    @Test
    void abortedGameIsFinishedButAFinishedGameStaysAsItIs() throws GameException {
        assertEquals(GameStatus.ABORTED, play(0, 0).aborted().status());
        GameState won = play(0, 0, 1, 0, 0, 1, 1, 1, 0, 2);
        assertEquals(GameStatus.X_WON, won.aborted().status());
    }
}
