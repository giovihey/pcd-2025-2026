package pcd.sketch01;

import pcd.sketch01.controller.GameController;
import pcd.sketch01.controller.MassiveBoardConf;
import pcd.sketch01.model.Board;
import pcd.sketch01.view.View;
import pcd.sketch01.view.ViewModel;

public class Sketch01 {

	public static void main(String[] argv) {

		/* 
		 * Different board configs to try:
		 * - minimal: 2 small balls
		 * - large: 400 small balls
		 * - massive: 4500 small balls 
		 */
		
		//var boardConf = new MinimalBoardConf();
		//var boardConf = new LargeBoardConf();
		 var boardConf = new MassiveBoardConf();

		Board board = new Board();
		board.init(boardConf);

		// --- View ---
		ViewModel viewModel = new ViewModel();
		View view = new View(viewModel, 1200, 800);

		// --- Controller (owns game loop, bot, input, game manager) ---
		GameController controller = new GameController(board, viewModel, view);
		controller.start();  // blocks until game ends

		System.exit(0);
	}
}
