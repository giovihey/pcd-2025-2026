package pcd.ass01.sketch01;

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
    	setTitle("Sketch 01");
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
        private int dx;
        private int dy;
        
        public VisualiserPanel(int w, int h){
            setSize(w,h + 25);
            dx = w/2;
            dy = h/2;
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
    		g2.drawLine(dx,0,dx,dy*2);
    		g2.drawLine(0,dy,dx*2,dy);
    		g2.setColor(Color.BLACK);
    		
    		    g2.setStroke(new BasicStroke(1));
	    		for (var b: model.getBalls()) {
	    			var p = b.pos();
	            	int x0 = (int)(dx + p.x()*dx);
	                int y0 = (int)(dy - p.y()*dy);
	                int radiusX = (int)(b.radius()*dx);
	                int radiusY = (int)(b.radius()*dy);
	                g2.drawOval(x0 - radiusX,y0 - radiusY,radiusX*2,radiusY*2);
	    		}
	
    		    g2.setStroke(new BasicStroke(3));
	    		var pb = model.getPlayerBall();
	    		if (pb != null) {
					var p1 = pb.pos();
		        	int x0 = (int)(dx + p1.x()*dx);
		            int y0 = (int)(dy - p1.y()*dy);
	                int radiusX = (int)(pb.radius()*dx);
	                int radiusY = (int)(pb.radius()*dy);
	                g2.drawOval(x0 - radiusX,y0 - radiusY,radiusX*2,radiusY*2);
	    		}
    		    
    		    sync.notifyFrameRendered();
    		
        }
        
    }
}
