package pcd.sketch01.controller;

import pcd.sketch01.model.Ball;
import pcd.sketch01.model.V2d;

import java.util.Random;

/**
 * BotController manages the behavior of the bot's ball
 */
public class BotController {

    private final Ball botBall;
    private final Random random;
    private long lastKickTime;
    private static final long KICK_DELAY_MS = 2000;  // 2 seconds between kicks
    private static final double VELOCITY_THRESHOLD = 0.05;  // velocity threshold to consider the ball stationary
    private static final double KICK_POWER = 1.5;  // kick power

    public BotController(Ball botBall, long initialTime) {
        this.botBall = botBall;
        this.random = new Random();
        this.lastKickTime = initialTime;
    }

    /**
     * Updates the bot's behavior.
     * If the ball is stationary and at least KICK_DELAY_MS milliseconds have passed,
     * the bot kicks the ball in a random direction (0-360 degrees)
     */
    public void update() {
        long now = System.currentTimeMillis();

        if (botBall.getVel().abs() < VELOCITY_THRESHOLD &&
                now - lastKickTime > KICK_DELAY_MS) {

            // Generate a random angle between 0 and 2π (360 degrees)
            // This ensures the bot can move in any direction
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
