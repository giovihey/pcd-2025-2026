package pcd.sketch01;

import java.util.ArrayList;

record BallViewInfo(P2d pos, double radius) {}
record HoleViewInfo(P2d pos, double radius) {}

public class ViewModel {

	private final ArrayList<BallViewInfo> balls;
	private final ArrayList<HoleViewInfo> holes;
	private BallViewInfo player, bot;
	private int framePerSec;
	private int playerScore, botScore;
	
	public ViewModel() {
		balls = new ArrayList<BallViewInfo>();
		holes = new ArrayList<HoleViewInfo>();
		framePerSec = 0;
	}
	
	public synchronized void update(Board board, int framePerSec) {
		balls.clear();
		for (var b: board.getBalls()) {
			balls.add(new BallViewInfo(b.getPos(), b.getRadius()));
		}
		holes.clear();
		for (var h: board.getHoles()) {
			holes.add(new HoleViewInfo(h.pos(), h.radius()));
		}
		this.framePerSec = framePerSec;
		this.playerScore = board.getPlayerScore();
		this.botScore = board.getBotScore();
		var p = board.getPlayerBall();
		var b =  board.getBotBall();
		player = new BallViewInfo(p.getPos(), p.getRadius());
		bot = new BallViewInfo(b.getPos(), b.getRadius());
	}
	
	public synchronized ArrayList<BallViewInfo> getBalls(){
		var copy = new ArrayList<BallViewInfo>();
		copy.addAll(balls);
		return copy;
	}

	public synchronized ArrayList<HoleViewInfo> getHoles(){
		var copy = new ArrayList<HoleViewInfo>();
		copy.addAll(holes);
		return copy;
	}

	public synchronized int getFramePerSec() {
		return framePerSec;
	}

	public synchronized BallViewInfo getPlayerBall() {
		return player;
	}

	public synchronized BallViewInfo getBotBall() {
		return bot;
	}
	
	public synchronized int getPlayerScore() { return playerScore; }
	public synchronized int getBotScore() { return botScore; }
}
