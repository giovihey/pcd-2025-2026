package pcd.sketch01.controller;

import pcd.sketch01.model.*;

import java.util.ArrayList;
import java.util.List;

public class MinimalBoardConf implements BoardConf {

	@Override
	public Ball getPlayerBall() {
    	return new Ball(new P2d(-0.5,0), 0.06, 1, new V2d(0,0));
	}

	@Override
	public Ball getBotBall() { return  new Ball(new P2d(0.5, 0), 0.05, 1.5, new V2d(0,1)); }

	@Override
	public List<Ball> getSmallBalls() {		
        var balls = new ArrayList<Ball>();
    	var b1 = new Ball(new P2d(0, 0.5), 0.05, 0.75, new V2d(0,0));
    	var b2 = new Ball(new P2d(0.05, 0.55), 0.025, 0.25, new V2d(0,0));
    	balls.add(b1);
    	balls.add(b2);
    	return balls;
	}

	@Override
	public List<Hole> getHoles() {
		var holes = new ArrayList<Hole>();
		holes.add(new Hole(new P2d(-1.45, 0.95), 0.3));
		holes.add(new Hole(new P2d(1.45, 0.95), 0.3));
		return holes;
	}

	@Override
	public Boundary getBoardBoundary() {
        return new Boundary(-1.5,-1.0,1.5,1.0);
	}

}
