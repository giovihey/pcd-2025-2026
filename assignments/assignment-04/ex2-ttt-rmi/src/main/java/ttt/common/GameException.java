package ttt.common;

/** Application error (unknown game, name taken, illegal move...). Checked and serializable, so it travels over RMI. */
public class GameException extends Exception {

    public GameException(String message) {
        super(message);
    }
}
