package ttt.client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

import ttt.common.GameException;
import ttt.common.GameServer;
import ttt.common.GameState;
import ttt.common.GameStatus;
import ttt.common.PlayerListener;
import ttt.common.Symbol;

/**
 * Console client.
 * Commands: list | create NAME | join NAME | ROW COL (1-3) | leave | quit
 * <p>
 * The client is itself a remote object (PlayerListener): the server calls onGameChanged on it, on an
 * RMI thread, while the main thread reads the commands. So the fields shared by the two are volatile.
 */
public class ClientMain implements PlayerListener {

    private final GameServer server;
    private PlayerListener myStub;
    private volatile String gameName;
    private volatile Symbol mine;

    private ClientMain(GameServer server) {
        this.server = server;
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;

        GameServer server = (GameServer) LocateRegistry.getRegistry(host, port).lookup(GameServer.NAME);
        ClientMain client = new ClientMain(server);
        client.myStub = (PlayerListener) UnicastRemoteObject.exportObject(client, 0);
        client.run();
        UnicastRemoteObject.unexportObject(client, true);
    }

    private void run() {
        System.out.println("Commands: list | create NAME | join NAME | ROW COL (1-3) | leave | quit");
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String[] t = in.nextLine().trim().split("\\s+");
            try {
                switch (t[0]) {
                    case "list" -> printOpenGames();
                    case "create" -> create(arg(t));
                    case "join" -> join(arg(t));
                    case "leave" -> leave();
                    case "quit" -> {
                        leave();
                        return;
                    }
                    default -> move(t);
                }
            } catch (GameException e) {
                System.out.println("! " + e.getMessage());
            } catch (RemoteException e) {
                System.out.println("! Connection problem: " + e.getMessage());
            }
        }
        try {
            leave();
        } catch (RemoteException ignored) {
            // server not reachable
        }
    }

    private void printOpenGames() throws RemoteException {
        List<String> open = server.listOpenGames();
        System.out.println(open.isEmpty() ? "(no open games)" : "Open games: " + String.join(", ", open));
    }

    private void create(String name) throws RemoteException, GameException {
        checkNotPlaying();
        // set BEFORE the call: callbacks can arrive as soon as the server knows about us
        gameName = name;
        mine = Symbol.X;
        try {
            server.createGame(name, myStub);
            System.out.println("Game '" + name + "' created, waiting for an opponent...");
        } catch (RemoteException | GameException e) {
            gameName = null;
            throw e;
        }
    }

    private void join(String name) throws RemoteException, GameException {
        checkNotPlaying();
        gameName = name;
        mine = Symbol.O;
        try {
            show(server.joinGame(name, myStub));
        } catch (RemoteException | GameException e) {
            gameName = null;
            throw e;
        }
    }

    private void move(String[] t) throws RemoteException, GameException {
        String game = gameName;
        if (game == null) {
            System.out.println("Unknown command. Use: list | create NAME | join NAME | quit");
            return;
        }
        try {
            int row = Integer.parseInt(t[0]) - 1;
            int col = Integer.parseInt(t[1]) - 1;
            show(server.makeMove(game, mine, row, col));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Type ROW COL (numbers 1-3), or 'leave'.");
        }
    }

    private void leave() throws RemoteException {
        String game = gameName;
        if (game != null) {
            gameName = null;
            server.leaveGame(game, mine);
            System.out.println("You left the game.");
        }
    }

    /** Called by the server (RMI thread): the opponent did something. */
    @Override
    public void onGameChanged(GameState state) {
        show(state);
    }

    private void show(GameState s) {
        String message;
        GameStatus status = s.status();
        if (status == GameStatus.WAITING_FOR_OPPONENT) {
            message = "Waiting for an opponent...";
        } else if (status == GameStatus.IN_PROGRESS) {
            message = (s.turn() == mine) ? "Your turn: type ROW COL (1-3)" : "Opponent's turn...";
        } else if (status == GameStatus.DRAW) {
            message = "*** Draw ***";
        } else if (status == GameStatus.ABORTED) {
            message = "*** The opponent left: game over ***";
        } else {
            boolean iWon = (status == GameStatus.X_WON) == (mine == Symbol.X);
            message = iWon ? "*** You won! ***" : "*** You lost ***";
        }
        if (s.isFinished()) {
            gameName = null;
        }
        System.out.println(s + "\n" + message);   // one println: lines of different threads do not mix
    }

    private void checkNotPlaying() throws GameException {
        if (gameName != null) {
            throw new GameException("You are already in game '" + gameName + "' (type 'leave' first)");
        }
    }

    private static String arg(String[] t) throws GameException {
        if (t.length < 2) {
            throw new GameException("Missing game name");
        }
        return t[1];
    }
}
