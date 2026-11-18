package ttt.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote object exported by each CLIENT (callback). The server calls it to tell a player what the
 * OPPONENT did: the opponent joined, made a move, or left. A player is never notified of its own
 * moves: it gets the new state as the return value of the call.
 */
public interface PlayerListener extends Remote {

    void onGameChanged(GameState state) throws RemoteException;
}
