package pcd.ass01.sketch01;


public class View {

	private ViewFrame frame;
	
	public View(ViewModel model) {
		frame = new ViewFrame(model, 800, 800);	
		frame.setVisible(true);
	}
		
	public void render() {
		frame.render();
	}
}
