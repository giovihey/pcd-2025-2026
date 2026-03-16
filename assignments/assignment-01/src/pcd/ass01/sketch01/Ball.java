package pcd.ass01.sketch01;

public class Ball {
    
    private P2d pos;
    private V2d vel;
    private double radius;
    private double mass;   
    
    private static double FRICTION_FACTOR = 0.015; // 0.01 min 0.02 buono
    
    public Ball(P2d pos, double radius, double mass, V2d vel){
       this.pos = pos;
       this.radius = radius;
       this.mass = mass;
       this.vel = vel;
    }

    public void updateState(long dt, Board ctx){
        double speed = vel.abs();
        double dt_scaled = dt*0.001;
    	if (speed > 0.001) {
            double dec    = FRICTION_FACTOR * dt_scaled;
            double factor = Math.max(0, speed - dec) / speed;
            vel = vel.mul(factor);
        } else {
        	vel = new V2d(0,0);
        }
        pos = pos.sum(vel.mul(dt_scaled));
     	applyBoundaryConstraints(ctx);
   }

    private void applyBoundaryConstraints(Board ctx){
        Boundary bounds = ctx.getBounds();
        if (pos.x() + radius > bounds.x1()){
            pos = new P2d(bounds.x1() - radius, pos.y());
            vel = vel.getSwappedX();
        } else if (pos.x() - radius < bounds.x0()){
            pos = new P2d(bounds.x0() + radius, pos.y());
            vel = vel.getSwappedX();
        } else if (pos.y() + radius > bounds.y1()){
            pos = new P2d(pos.x(), bounds.y1() - radius);
            vel = vel.getSwappedY();
        } else if (pos.y() - radius < bounds.y0()){
            pos = new P2d(pos.x(), bounds.y0() + radius);
            vel = vel.getSwappedY();
        }
    }

    public static void resolveCollision(Ball a, Ball b) {
        double dx   = b.pos.x() - a.pos.x();
        double dy   = b.pos.y() - a.pos.y();
        double dist = Math.hypot(dx, dy);
        double minD = a.radius + b.radius;
        
        if (dist >= minD || dist < 1e-6)  /* not colliding or null dist*/
        	return;

        /* update pos due the collision */ 
        
        double nx = dx / dist;
        double ny = dy / dist;

        double overlap = minD - dist;
        double totalM  = a.mass + b.mass;

        double a_factor = overlap * (b.mass / totalM);
        double a_deltax = nx * a_factor; 
        double a_deltay = ny * a_factor; 
        
        a.pos = new P2d(a.getPos().x() - a_deltax, a.getPos().y() - a_deltay);
        
        double b_factor = overlap * (a.mass / totalM);
        double b_deltax = nx * b_factor; 
        double b_deltay = ny * b_factor; 

        b.pos = new P2d(b.getPos().x() + b_deltax, b.getPos().y() + b_deltay);

        /* update velocity vectors */
        
        /* relative speed along the normal vector*/

        double dvx = b.vel.x() - a.vel.x();
        double dvy = b.vel.y() - a.vel.y(); 
        double dvn = dvx * nx + dvy * ny;

        if (dvn > 0) /* already separating*/ 
        	return; 
        
        double e   = 1.0; // restitution (1 max)
        double imp = -(1 + e) * dvn / (1.0/a.getMass() + 1.0/b.getMass());        
        a.vel = new V2d(a.vel.x() - (imp / a.mass) * nx, a.vel.y() - (imp / a.mass) * ny);                
        b.vel = new V2d(b.vel.x() + (imp / b.mass) * nx, a.vel.y() + (imp / b.mass) * ny);
    }

    
    public P2d getPos(){        
    	return pos;
    }
    
    public double getMass() {
    	return mass;
    }
    
    public V2d getVel() {
    	return vel;
    }
    
    public double getRadius() {
    	return radius;
    }

}
