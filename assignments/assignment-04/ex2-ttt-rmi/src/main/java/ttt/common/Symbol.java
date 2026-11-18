package ttt.common;

/** The mark of a player. X is the creator of the game and moves first. */
public enum Symbol {
    X, O;

    public Symbol opponent() {
        return this == X ? O : X;
    }
}
