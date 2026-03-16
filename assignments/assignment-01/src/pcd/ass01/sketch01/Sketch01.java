package pcd.ass01.sketch01;

public class Sketch01 {

	
	public static void main(String[] argv) {

		Board board = new Board();
		board.init();
		
		ViewModel viewModel = new ViewModel();
		View view = new View(viewModel);
				
		viewModel.init();
		
		long lastUpdateTime = System.currentTimeMillis();
				
		while (true){			
		
			/* update board state */
			
			long elapsed = System.currentTimeMillis() - lastUpdateTime;
			lastUpdateTime = System.currentTimeMillis();			
			board.updateState(elapsed);
			
			/* render */
			
			viewModel.update(board);			
			view.render();
			
		}
	}
	
}
