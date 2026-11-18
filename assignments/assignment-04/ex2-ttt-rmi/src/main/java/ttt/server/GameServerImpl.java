package ttt.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ttt.common.GameException;
import ttt.common.GameServer;
import ttt.common.GameState;
import ttt.common.PlayerListener;
import ttt.common.Symbol;

/**
 * Remote object of the server.
 * The only shared data is a ConcurrentHashMap of games, and each game is a monitor.
 * <p>
 * Callbacks to clients are always made OUTSIDE the monitor of the game, so a slow client can never
 * block the other games. Only the opponent of the player who acted is notified;
 * the player who acted gets the new state as return value.
 */
public class GameServerImpl implements GameServer {

    private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

    @Override
    public void createGame(String name, PlayerListener creator) throws GameException {
        if (name == null || name.isBlank()) {
            throw new GameException("The game needs a name");
        }
        // atomic: of many concurrent creations of the same name only one succeeds
        if (games.putIfAbsent(name, new Game(creator)) != null) {
            throw new GameException("A game named '" + name + "' already exists");
        }
    }

    @Override
    public GameState joinGame(String name, PlayerListener opponent) throws GameException {
        Game game = find(name);
        GameState state = game.join(opponent);          // atomic: only one joiner succeeds
        return tell(name, game, Symbol.X, state);       // the creator learns that the game started
    }

    @Override
    public GameState makeMove(String name, Symbol player, int row, int col) throws GameException {
        Game game = find(name);
        GameState state = game.move(player, row, col);  // atomic validation + update
        return tell(name, game, player.opponent(), state);
    }

    @Override
    public void leaveGame(String name, Symbol player) {
        Game game = games.get(name);
        if (game == null) {
            return;
        }
        if (game.abort()) {
            tell(name, game, player.opponent(), game.state());
        }
        games.remove(name, game);
    }

    @Override
    public List<String> listOpenGames() {
        List<String> open = new ArrayList<>();
        for (Map.Entry<String, Game> e : games.entrySet()) {
            if (e.getValue().isOpen()) {
                open.add(e.getKey());
            }
        }
        Collections.sort(open);
        return open;
    }

    private Game find(String name) throws GameException {
        Game game = games.get(name);
        if (game == null) {
            throw new GameException("No game named '" + name + "'");
        }
        return game;
    }

    /**
     * Notifies a player (callback, no lock held). If the player is unreachable the game is aborted.
     * A finished game is removed, which frees its name. Returns the state to give back to the caller.
     */
    private GameState tell(String name, Game game, Symbol who, GameState state) {
        PlayerListener listener = game.listenerOf(who);
        if (listener != null) {
            try {
                listener.onGameChanged(state);
            } catch (RemoteException e) {
                game.abort();
                state = game.state();
            }
        }
        if (state.isFinished()) {
            games.remove(name, game);
        }
        return state;
    }
}
