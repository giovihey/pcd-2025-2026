package pcd.sketch01;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class ViewFrame extends JFrame {
    
    private VisualiserPanel panel;
    private ViewModel model;
    private RenderSynch sync;
    
    public ViewFrame(ViewModel model, int w, int h){
    	this.model = model;
    	this.sync = new RenderSynch();
    	setTitle("Sketch 03");
        setSize(w,h + 25);
        setResizable(false);
        panel = new VisualiserPanel(w,h);
        getContentPane().add(panel);
        addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent ev){
				System.exit(-1);
			}
			public void windowClosed(WindowEvent ev){
				System.exit(-1);
			}
		});
    }
     
    public void render(){
		long nf = sync.nextFrameToRender();
        panel.repaint();
		try {
			sync.waitForFrameRendered(nf);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
    }
        
    public class VisualiserPanel extends JPanel {
        private int ox;
        private int oy;
        private int delta;
        
        public VisualiserPanel(int w, int h){
            setSize(w,h + 25);
            ox = w/2;
            oy = h/2;
            delta = Math.min(ox, oy);
        }

		/**
		 * Draws a ball on the graphics context with the specified stroke width and optional label.
		 * @param g2 the Graphics2D object to draw on
		 * @param ball the BallViewInfo containing position and radius
		 * @param strokeWidth the width of the stroke for the ball's outline
		 * @param label the text label to draw inside the ball, or null/empty for no label
		 */
        private void drawBallWithLabel(Graphics2D g2, BallViewInfo ball, int strokeWidth, String label) {
            if (ball != null) {
                var p1 = ball.pos();
                int x0 = (int)(ox + p1.x()*delta);
                int y0 = (int)(oy - p1.y()*delta);
                int radiusX = (int)(ball.radius()*delta);
                int radiusY = (int)(ball.radius()*delta);
                g2.setStroke(new BasicStroke(strokeWidth));
                g2.drawOval(x0 - radiusX, y0 - radiusY, radiusX*2, radiusY*2);
                if (label != null && !label.isEmpty()) {
                    g2.drawString(label, x0 - 3, y0 + 5);
                }
            }
        }

        public void paint(Graphics g){
    		Graphics2D g2 = (Graphics2D) g;
    		
    		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    		          RenderingHints.VALUE_ANTIALIAS_ON);
    		g2.setRenderingHint(RenderingHints.KEY_RENDERING,
    		          RenderingHints.VALUE_RENDER_QUALITY);
    		g2.clearRect(0,0,this.getWidth(),this.getHeight());
            
    		g2.setColor(Color.LIGHT_GRAY);
		    g2.setStroke(new BasicStroke(1));
    		g2.drawLine(ox,0,ox,oy*2);
    		g2.drawLine(0,oy,ox*2,oy);
    		g2.setColor(Color.BLACK);
    		
	    		for (var b: model.getBalls()) {
	    		    drawBallWithLabel(g2, b, 1, "");
	    		}
	
    		    drawBallWithLabel(g2, model.getPlayerBall(), 3, "H");
    		    drawBallWithLabel(g2, model.getBotBall(), 3, "B");
    		    
    		    g2.setStroke(new BasicStroke(1));
	    		g2.drawString("Num small balls: " + model.getBalls().size(), 20, 40);
	    		g2.drawString("Frame per sec: " + model.getFramePerSec(), 20, 60);

	    		sync.notifyFrameRendered();
        }
    }
}
