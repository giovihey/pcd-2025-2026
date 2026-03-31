package pcd.sketch01.model;

/**
 * GameManager handles the game state and determines end-of-game conditions.
 */
public class GameManager {

    private final Board board;
    private boolean gameEnded = false;
    private String winner = null;

    public GameManager(Board board) {
        this.board = board;
    }

    /**
     * Updates the game state and checks for end-of-game conditions.
     */
    public void updateGameState() {
        if (gameEnded) {
            return;
        }

        checkGameEndConditions();
    }

    /**
     * Checks end-of-game conditions:
     * 1. If the player's ball is inside a hole -> bot wins
     * 2. If the bot's ball is inside a hole -> player wins
     * 3. If there are no more small balls -> the player with the highest score wins
     */
    private void checkGameEndConditions() {
        Ball playerBall = board.getPlayerBall();
        Ball botBall = board.getBotBall();

        // Conditions 1 & 2: check if either main ball has fallen into a hole
        for (Hole hole : board.getHoles()) {
            if (isBallInHole(playerBall, hole)) {
                endGame("BOT");
                return;
            }
            if (isBallInHole(botBall, hole)) {
                endGame("PLAYER");
                return;
            }
        }

        // Condition 3: no small balls remaining — winner is determined by score
        if (board.getBalls().isEmpty()) {
            int playerScore = board.getPlayerScore();
            int botScore = board.getBotScore();

            if (playerScore > botScore) {
                endGame("PLAYER");
            } else if (botScore > playerScore) {
                endGame("BOT");
            } else {
                endGame("DRAW");
            }
        }
    }

    /**
     * Returns true if the given ball is fully inside the given hole.
     */
    private boolean isBallInHole(Ball ball, Hole hole) {
        double dist = Math.hypot(
                ball.getPos().x() - hole.pos().x(),
                ball.getPos().y() - hole.pos().y()
        );
        return dist <= hole.radius() - ball.getRadius();
    }

    /**
     * Marks the game as ended and sets the winner.
     */
    private void endGame(String winner) {
        this.gameEnded = true;
        this.winner = winner;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public String getWinner() {
        return winner;
    }

    public void printGameResult() {
        if (gameEnded) {
            System.out.println("\n=== GAME OVER ===");
            System.out.println("Winner: " + winner);
            System.out.println("Player Score: " + board.getPlayerScore());
            System.out.println("Bot Score: " + board.getBotScore());
        }
    }
}
