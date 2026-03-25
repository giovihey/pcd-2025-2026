package pcd.sketch01;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

/**
 * InputController handles keyboard input from the player.
 */
public class InputController {

    private final Ball playerBall;

    public InputController(Ball playerBall) {
        this.playerBall = playerBall;
    }

    /**
     * Registers a KeyListener on the frame to handle arrow key events.
     * @param frame the JFrame on which to register the listener
     */
    public void registerKeyListener(JFrame frame) {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
        });
        // Ensure the frame has focus so it can receive key events
        frame.setFocusable(true);
        frame.requestFocusInWindow();
    }

    /**
     * Handles arrow key presses and applies an impulse to the player's ball.
     * UP:    applies impulse (0,  1)
     * DOWN:  applies impulse (0, -1)
     * LEFT:  applies impulse (-1, 0)
     * RIGHT: applies impulse ( 1, 0)
     * Any other key is ignored.
     */
    private void handleKeyPressed(KeyEvent e) {
        V2d impulse = switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> new V2d(0, 1);
            case KeyEvent.VK_DOWN -> new V2d(0, -1);
            case KeyEvent.VK_LEFT -> new V2d(-1, 0);
            case KeyEvent.VK_RIGHT -> new V2d(1, 0);
            default -> null;
        };

        // Only kick if a valid arrow key was pressed
        if (impulse != null) {
            playerBall.kick(impulse);
        }
    }
}