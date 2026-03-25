package pcd.sketch01;

import java.util.Random;

/**
 * BotController gestisce il comportamento della palla del bot
 */
public class BotController {
    
    private final Ball botBall;
    private final Random random;
    private long lastKickTime;
    private static final long KICK_DELAY_MS = 2000;  // 2 secondi tra i calci
    private static final double VELOCITY_THRESHOLD = 0.05;  // soglia di velocità per considerare la palla ferma
    private static final double KICK_POWER = 1.5;  // potenza del calcio
    
    public BotController(Ball botBall, long initialTime) {
        this.botBall = botBall;
        this.random = new Random();
        this.lastKickTime = initialTime;
    }
    
    /**
     * Aggiorna il comportamento del bot
     * Se la palla è ferma e sono passati almeno KICK_DELAY_MS millisecondi,
     * il bot calcia la palla in una direzione casuale (0-360 gradi)
     */
    public void update() {
        long now = System.currentTimeMillis();
        
        if (botBall.getVel().abs() < VELOCITY_THRESHOLD && 
            now - lastKickTime > KICK_DELAY_MS) {
            
            // Genera un angolo casuale tra 0 e 2π (360 gradi)
            // Questo assicura che il bot possa andare in tutte le direzioni
            double angle = random.nextDouble() * Math.PI * 2;
            
            V2d velocity = new V2d(
                Math.cos(angle) * KICK_POWER,
                Math.sin(angle) * KICK_POWER
            );
            
            botBall.kick(velocity);
            lastKickTime = now;
        }
    }
}

