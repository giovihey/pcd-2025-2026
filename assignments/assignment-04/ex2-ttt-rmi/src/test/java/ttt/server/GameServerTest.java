package ttt.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import ttt.common.GameException;
import ttt.common.GameState;
import ttt.common.GameStatus;
import ttt.common.PlayerListener;
import ttt.common.Symbol;

/** Tests of the server object WITHOUT RMI: the concurrency is the same, as RMI calls it from many threads. */
class GameServerTest {

    /** A fake client: remembers what the server tells it. */
    static class Recorder implements PlayerListener {
        final List<GameState> received = new CopyOnWriteArrayList<>();
        private volatile GameState latest;

        @Override
        public void onGameChanged(GameState state) {
            received.add(state);
            update(state);
        }

        /** Keeps the most recent state (the one with more marks), whatever the way it was learnt. */
        synchronized void update(GameState state) {
            if (latest == null || marks(state) >= marks(latest)) {
                latest = state;
            }
        }

        GameState latest() {
            return latest;
        }
    }

    static int marks(GameState s) {
        int n = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (s.cellAt(r, c) != null) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Runs the task in 'threads' threads started at the same moment; returns how many did not throw. */
    static int race(int threads, Callable<?> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier start = new CyclicBarrier(threads);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    task.call();
                    return true;
                } catch (GameException e) {
                    return false;
                }
            }));
        }
        int successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get(10, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        pool.shutdown();
        return successes;
    }

    @Test
    void onlyOneOfManyConcurrentCreationsSucceeds() throws Exception {
        GameServerImpl server = new GameServerImpl();
        assertEquals(1, race(32, () -> {
            server.createGame("g", new Recorder());
            return null;
        }));
    }

    @Test
    void onlyOneOfManyConcurrentJoinsSucceeds() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("g", new Recorder());
        assertEquals(1, race(32, () -> server.joinGame("g", new Recorder())));
    }

    @Test
    void identicalConcurrentMovesAreAppliedOnlyOnce() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("g", new Recorder());
        server.joinGame("g", new Recorder());
        assertEquals(1, race(32, () -> server.makeMove("g", Symbol.X, 1, 1)));
    }

    @Test
    void onlyTheOpponentIsNotifiedOfAMove() throws Exception {
        GameServerImpl server = new GameServerImpl();
        Recorder x = new Recorder(), o = new Recorder();
        server.createGame("g", x);
        server.joinGame("g", o);                               // x is told that the game started

        GameState afterMove = server.makeMove("g", Symbol.X, 0, 0);

        assertEquals(1, marks(afterMove));                     // the mover gets the state as return value
        assertEquals(1, x.received.size());                    // ...and is not notified
        assertEquals(1, o.received.size());                    // the opponent is
        assertEquals(1, marks(o.received.get(0)));
    }

    @Test
    void finishedGameIsRemovedAndItsNameCanBeReused() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("g", new Recorder());
        server.joinGame("g", new Recorder());
        server.makeMove("g", Symbol.X, 0, 0);
        server.makeMove("g", Symbol.O, 1, 0);
        server.makeMove("g", Symbol.X, 0, 1);
        server.makeMove("g", Symbol.O, 1, 1);
        GameState end = server.makeMove("g", Symbol.X, 0, 2);

        assertEquals(GameStatus.X_WON, end.status());
        server.createGame("g", new Recorder());                // the name is free again
        assertEquals(List.of("g"), server.listOpenGames());
    }

    @Test
    void openGamesAreListedUntilSomebodyJoins() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("b", new Recorder());
        server.createGame("a", new Recorder());
        assertEquals(List.of("a", "b"), server.listOpenGames());
        server.joinGame("a", new Recorder());
        assertEquals(List.of("b"), server.listOpenGames());
    }

    @Test
    void unreachableOpponentAbortsTheGame() throws Exception {
        GameServerImpl server = new GameServerImpl();
        PlayerListener dead = s -> {
            throw new RemoteException("client crashed");
        };
        server.createGame("g", new Recorder());
        server.joinGame("g", dead);

        GameState s = server.makeMove("g", Symbol.X, 0, 0);    // the callback to O fails

        assertEquals(GameStatus.ABORTED, s.status());
        server.createGame("g", new Recorder());                // game removed, name free
    }

    @Test
    void unreachableCreatorIsDetectedWhenTheOpponentJoins() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("g", s -> {
            throw new RemoteException("creator crashed");
        });
        assertEquals(GameStatus.ABORTED, server.joinGame("g", new Recorder()).status());
    }

    @Test
    void leavingNotifiesTheOpponent() throws Exception {
        GameServerImpl server = new GameServerImpl();
        Recorder x = new Recorder();
        server.createGame("g", x);
        server.joinGame("g", new Recorder());

        server.leaveGame("g", Symbol.O);

        assertEquals(GameStatus.ABORTED, x.latest().status());
        assertTrue(server.listOpenGames().isEmpty());
    }

    @Test
    void leavingAGameThatIsStillWaitingRemovesIt() throws Exception {
        GameServerImpl server = new GameServerImpl();
        server.createGame("g", new Recorder());
        server.leaveGame("g", Symbol.X);
        assertFalse(server.listOpenGames().contains("g"));
    }

    // ---- many games at the same time, each played by two threads ----

    @Test
    void manyConcurrentGamesStayConsistent() throws Exception {
        GameServerImpl server = new GameServerImpl();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> games = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String name = "game" + i;
            games.add(pool.submit(() -> {
                playRandomGame(server, name);
                return null;
            }));
        }
        for (Future<?> g : games) {
            g.get(60, TimeUnit.SECONDS);                       // rethrows a failed assertion
        }
        pool.shutdown();
        assertTrue(server.listOpenGames().isEmpty());
    }

    private static void playRandomGame(GameServerImpl server, String name) throws Exception {
        Recorder x = new Recorder(), o = new Recorder();
        server.createGame(name, x);
        o.update(server.joinGame(name, o));

        Thread tx = new Thread(() -> player(server, name, Symbol.X, x));
        Thread to = new Thread(() -> player(server, name, Symbol.O, o));
        tx.start();
        to.start();
        tx.join();
        to.join();

        assertTrue(x.latest().isFinished());
        assertEquals(x.latest().status(), o.latest().status());
        // each player is notified only of the opponent's moves, in order: strictly more marks each time
        for (Recorder r : List.of(x, o)) {
            for (int i = 1; i < r.received.size(); i++) {
                assertTrue(marks(r.received.get(i)) > marks(r.received.get(i - 1)));
            }
        }
    }

    /** Plays random free cells whenever it is its turn, until the game is over. */
    private static void player(GameServerImpl server, String name, Symbol me, Recorder rec) {
        try {
            while (true) {
                GameState s = rec.latest();
                if (s != null && s.isFinished()) {
                    return;
                }
                if (s != null && s.status() == GameStatus.IN_PROGRESS && s.turn() == me) {
                    List<int[]> free = new ArrayList<>();
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            if (s.cellAt(r, c) == null) {
                                free.add(new int[] {r, c});
                            }
                        }
                    }
                    int[] cell = free.get(ThreadLocalRandom.current().nextInt(free.size()));
                    rec.update(server.makeMove(name, me, cell[0], cell[1]));
                } else {
                    Thread.yield();
                }
            }
        } catch (GameException e) {
            throw new AssertionError("a legal move was rejected: " + e.getMessage(), e);
        }
    }
}
