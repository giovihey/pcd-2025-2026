package pcd.sketch01;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

/**
 * InputController gestisce l'input da tastiera del player
 */
public class InputController {
    
    private final Ball playerBall;
    
    public InputController(Ball playerBall) {
        this.playerBall = playerBall;
    }
    
    /**
     * Registra il KeyListener sulla frame per gestire i tasti freccia
     * @param frame la JFrame sulla quale registrare il listener
     */
    public void registerKeyListener(JFrame frame) {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
        });
        // Assicura che il focus sia sulla frame per ricevere gli eventi
        frame.setFocusable(true);
    }
    
    /**
     * Gestisce la pressione dei tasti freccia
     * UP: aggiunge impulso (0, 1) alla velocità
     * DOWN: aggiunge impulso (0, -1) alla velocità
     * LEFT: aggiunge impulso (-1, 0) alla velocità
     * RIGHT: aggiunge impulso (1, 0) alla velocità
     */
    private void handleKeyPressed(KeyEvent e) {
        V2d impulse = switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> new V2d(0, 1);
            case KeyEvent.VK_DOWN -> new V2d(0, -1);
            case KeyEvent.VK_LEFT -> new V2d(-1, 0);
            case KeyEvent.VK_RIGHT -> new V2d(1, 0);
            default -> null;
        };

        if (impulse != null) {
            // Somma l'impulso alla velocità attuale della palla
            V2d currentVel = playerBall.getVel();
            V2d newVel = currentVel.sum(impulse);
            playerBall.kick(newVel);
        }
    }
}

