package pcd.sketch01;

import java.util.*;

public class Board {

    private List<Ball> balls;
    private Ball playerBall;
    private Ball botBall;
    private List<Hole> holes;
    private Boundary bounds;

    private int playerScore = 0;
    private int botScore = 0;
    
    public Board(){} 
    
    public void init(BoardConf conf) {
    	balls = conf.getSmallBalls();    	
    	playerBall = conf.getPlayerBall();
        botBall = conf.getBotBall();
        holes = conf.getHoles();
    	bounds = conf.getBoardBoundary();
    }

    public void updateState(long dt) {
        playerBall.updateState(dt, this);
        botBall.updateState(dt, this);

        for (var b : balls) b.updateState(dt, this);

        for (int i = 0; i < balls.size() - 1; i++)
            for (int j = i + 1; j < balls.size(); j++)
                Ball.resolveCollision(balls.get(i), balls.get(j), this);

        for (var b : balls) Ball.resolveCollision(playerBall, b, this);
        for (var b : balls) Ball.resolveCollision(botBall, b, this);

        Ball.resolveCollision(playerBall, botBall, this);

        // Score and remove balls that fall into holes
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            for (Hole hole : holes) {
                double dx   = ball.getPos().x() - hole.pos().x();
                double dy   = ball.getPos().y() - hole.pos().y();
                double dist = Math.hypot(dx, dy);
                if (dist < hole.radius()) {
                    if      (ball.getLastHitter() == Hitter.PLAYER) incrementPlayerScore();
                    else if (ball.getLastHitter() == Hitter.BOT)    incrementBotScore();
                    it.remove();
                    break;
                }
            }
        }
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public Ball getPlayerBall() {
        return playerBall;
    }

    public Ball getBotBall() {
        return botBall;
    }

    public List<Hole> getHoles() {
        return holes;
    }

    public Boundary getBounds() {
        return bounds;
    }

    public int getPlayerScore() { return playerScore; }

    public int getBotScore() { return botScore; }

    public void incrementPlayerScore() { playerScore++; }

    public void incrementBotScore() { botScore++; }
}
