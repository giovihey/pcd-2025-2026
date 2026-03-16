package pcd.ass01.sketch01;

import java.util.*;

public class Board {

    private Boundary bounds;
    private ArrayList<Ball> balls;    
    private Ball playerBall;
    
    public Board(){
        bounds = new Boundary(-1.0,-1.0,1.0,1.0);
        balls = new ArrayList<Ball>();
    } 
    
    public void init() {
    	
    	var b1 = new Ball(new P2d(0, 0.5), 0.05, 0.75, new V2d(0,0));
    	var b2 = new Ball(new P2d(0.05, 0.55), 0.025, 0.5, new V2d(0,0));
    	balls.add(b1);
    	balls.add(b2);
    	
    	playerBall = new Ball(new P2d(0, 0), 0.06, 1, new V2d(0,0.3)); 
    }
    
    public void updateState(long dt) {

    	playerBall.updateState(dt, this);
    	
    	for (var b: balls) {
    		b.updateState(dt, this);
    	}       	
    	
    	for (int i = 0; i < balls.size() - 1; i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                Ball.resolveCollision(balls.get(i), balls.get(j));
            }
        }
    	for (var b: balls) {
    		Ball.resolveCollision(playerBall, b);
    	} 
    	   	    	
    }
    
    public List<Ball> getBalls(){
    	return balls;
    }
    
    public Ball getPlayerBall() {
    	return playerBall;
    }
    
    public  Boundary getBounds(){
        return bounds;
    }
}
