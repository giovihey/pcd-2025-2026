package pcd.sketch01;

public class Ball {

    private static final double FRICTION_FACTOR    = 0.25;
    private static final double RESTITUTION_FACTOR = 1;

    private P2d pos;
    private V2d vel;
    private final double radius;
    private final double mass;

    private Hitter lastHitter = Hitter.NONE;

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
            vel = new V2d(0, 0);
        }
        pos = pos.sum(vel.mul(dt_scaled));
     	applyBoundaryConstraints(ctx);
    }

    public void kick(V2d vel) {
    	this.vel = vel;
    }

    /**
     * 
     * Keep the ball inside the boundaries, updating the velocity in the case of bounces
     * 
     * @param ctx
     */
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

    /**
     *  Resolving collision between 2 balls, updating their position and velocity
     * @param a
     * @param b
     * @param ctx
     */
    public static void resolveCollision(Ball a, Ball b, Board ctx) {
        double dx   = b.pos.x() - a.pos.x();
        double dy   = b.pos.y() - a.pos.y();
        double dist = Math.hypot(dx, dy);
        double minD = a.radius + b.radius;

        if (dist >= minD || dist <= 1e-6) return;

        double nx = dx / dist;
        double ny = dy / dist;

        // Resolve overlap (proportional to mass)
        double overlap  = minD - dist;
        double totalM   = a.mass + b.mass;

        double aFactor = overlap * (b.mass / totalM);
        a.pos = new P2d(a.pos.x() - nx * aFactor, a.pos.y() - ny * aFactor);

        double bFactor = overlap * (a.mass / totalM);
        b.pos = new P2d(b.pos.x() + nx * bFactor, b.pos.y() + ny * bFactor);

        // Update velocities
        double dvx = b.vel.x() - a.vel.x();
        double dvy = b.vel.y() - a.vel.y();
        double dvn = dvx * nx + dvy * ny;

        if (dvn <= 0) {
            double imp = -(1 + RESTITUTION_FACTOR) * dvn / (1.0 / a.mass + 1.0 / b.mass);
            a.vel = new V2d(a.vel.x() - (imp / a.mass) * nx, a.vel.y() - (imp / a.mass) * ny);
            b.vel = new V2d(b.vel.x() + (imp / b.mass) * nx, b.vel.y() + (imp / b.mass) * ny);
        }

        // Track last hitter for small balls
        boolean aIsSmall = a.radius < 0.05;
        boolean bIsSmall = b.radius < 0.05;

        if (aIsSmall && !bIsSmall) trackLastHitter(a, b, ctx);
        else if (bIsSmall && !aIsSmall) trackLastHitter(b, a, ctx);
    }

    private static void trackLastHitter(Ball small, Ball hitter, Board ctx) {
        if (hitter == ctx.getPlayerBall()) small.lastHitter = Hitter.PLAYER;
        else if (hitter == ctx.getBotBall()) small.lastHitter = Hitter.BOT;
    }

    public P2d getPos() {
        return pos;
    }

    public V2d getVel() {
        return vel;
    }

    public double getRadius() {
        return radius;
    }

    public Hitter getLastHitter() { return lastHitter; }

    public void setLastHitter(Hitter h) { this.lastHitter = h; }
}
