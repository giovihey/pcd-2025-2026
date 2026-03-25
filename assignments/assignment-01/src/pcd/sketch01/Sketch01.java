package pcd.sketch01;

public class Sketch01 {

	public static void main(String[] argv) {

		/* 
		 * Different board configs to try:
		 * - minimal: 2 small balls
		 * - large: 400 small balls
		 * - massive: 4500 small balls 
		 */
		
		// var boardConf = new MinimalBoardConf();
		var boardConf = new LargeBoardConf();
		// var boardConf = new MassiveBoardConf();

		Board board = new Board();
		board.init(boardConf);
		
		ViewModel viewModel = new ViewModel();
		View view = new View(viewModel, 1200, 800);

		// Setup input controller per gestire i tasti del player
		var inputController = new InputController(board.getPlayerBall());
		inputController.registerKeyListener(view.getFrame());
		
		// Setup game manager per gestire la logica di fine gioco
		var gameManager = new GameManager(board);
		
		// Setup bot controller per gestire il movimento del bot
		long t0 = System.currentTimeMillis();
		var botController = new BotController(board.getBotBall(), t0);
		
		viewModel.update(board, 0);			
		view.render();
		waitAbit();

		int nFrames = 0;
		long lastUpdateTime = System.currentTimeMillis();
				
		/* main simulation loop */
		while (!gameManager.isGameEnded()){
			// Update bot controller
			botController.update();

			/* update board state */
			long elapsed = System.currentTimeMillis() - lastUpdateTime;
			lastUpdateTime = System.currentTimeMillis();			
			board.updateState(elapsed);
			
			/* check game end conditions */
			gameManager.updateGameState();
			
			/* render */
			nFrames++;
			int framePerSec = 0;
			long dt = (System.currentTimeMillis() - t0);
			if (dt > 0) {
				framePerSec = (int)(nFrames*1000/dt);
			}

			viewModel.update(board, framePerSec);			
			view.render();
			
		}
		
		// Print game result and exit
		gameManager.printGameResult();
		System.exit(0);
	}

	private static void waitAbit() {
		try {
			Thread.sleep(2000);
		} catch (Exception ex) {}
	}
}
