package ttt.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** The remote object exposed by the server (bound in the RMI registry). */
public interface GameServer extends Remote {

    String NAME = "ttt-server";

    /** Creates a game and waits for an opponent. The creator plays X. */
    void createGame(String name, PlayerListener creator) throws RemoteException, GameException;

    /** Joins an existing game as O. Returns the state of the started game. */
    GameState joinGame(String name, PlayerListener opponent) throws RemoteException, GameException;

    /** Plays a move. Returns the state after the move. */
    GameState makeMove(String name, Symbol player, int row, int col) throws RemoteException, GameException;

    /** Abandons the game: the opponent is notified. */
    void leaveGame(String name, Symbol player) throws RemoteException;

    /** Names of the games waiting for an opponent. */
    List<String> listOpenGames() throws RemoteException;
}
