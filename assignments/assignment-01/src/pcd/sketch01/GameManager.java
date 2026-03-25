package pcd.sketch01;

/**
 * GameManager gestisce lo stato del gioco e determina le condizioni di fine partita
 */
public class GameManager {
    
    private final Board board;
    private boolean gameEnded = false;
    private String winner = null;
    
    public GameManager(Board board) {
        this.board = board;
    }
    
    /**
     * Aggiorna lo stato del gioco e controlla le condizioni di fine partita
     */
    public void updateGameState() {
        if (gameEnded) {
            return;
        }
        
        checkGameEndConditions();
    }
    
    /**
     * Verifica le condizioni di fine partita:
     * 1. Se la palla del player è dentro un buco -> bot vince
     * 2. Se la palla del bot è dentro un buco -> player vince
     * 3. Se non ci sono più small balls -> vince chi ha più punti
     */
    private void checkGameEndConditions() {
        Ball playerBall = board.getPlayerBall();
        Ball botBall = board.getBotBall();
        
        // Condizione 1 e 2: controllare se le palle dei player finiscono nei buchi
        for (Hole hole : board.getHoles()) {
            double distPlayer = Math.hypot(
                playerBall.getPos().x() - hole.getPos().x(),
                playerBall.getPos().y() - hole.getPos().y()
            );
            if (distPlayer < hole.getRadius()) {
                gameEnded = true;
                winner = "BOT";
                return;
            }
            
            double distBot = Math.hypot(
                botBall.getPos().x() - hole.getPos().x(),
                botBall.getPos().y() - hole.getPos().y()
            );
            if (distBot < hole.getRadius()) {
                gameEnded = true;
                winner = "PLAYER";
                return;
            }
        }
        
        // Condizione 3: controllare se non ci sono più small balls
        if (board.getBalls().isEmpty()) {
            gameEnded = true;
            int playerScore = board.getPlayerScore();
            int botScore = board.getBotScore();
            if (playerScore > botScore) {
                winner = "PLAYER";
            } else if (botScore > playerScore) {
                winner = "BOT";
            } else {
                winner = "DRAW";
            }
        }
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

