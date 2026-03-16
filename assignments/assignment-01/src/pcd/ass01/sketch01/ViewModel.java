package pcd.ass01.sketch01;

import java.util.ArrayList;

record BallViewInfo(P2d pos, double radius, V2d vel) {}

public class ViewModel {

	private ArrayList<BallViewInfo> balls;
	private BallViewInfo player;
	private long startTime;
	
	public ViewModel() {
		balls = new ArrayList<BallViewInfo>();
	}
	
	public void init() {
		startTime = System.currentTimeMillis();
	}
	
	public void update(Board board) {
		balls.clear();
		for (var b: board.getBalls()) {
			balls.add(new BallViewInfo(b.getPos(), b.getRadius(), b.getVel()));
		}
		var p = board.getPlayerBall();
		player = new BallViewInfo(p.getPos(), p.getRadius(), p.getVel());
	}
	
	public ArrayList<BallViewInfo> getBalls(){
		return balls;
	}
	
	public BallViewInfo getPlayerBall() {
		return player;
	}
	
	public long getStartTime() {
		return startTime;
	}
}
