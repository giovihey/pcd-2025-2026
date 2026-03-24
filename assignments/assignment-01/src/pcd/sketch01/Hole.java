package pcd.sketch01;

public class Hole {
    private P2d pos;
    private double radius;

    public Hole(P2d pos, double radius) {
        this.pos = pos;
        this.radius = radius;
    }

    public P2d getPos() {
        return pos;
    }

    public double getRadius() {
        return radius;
    }
}
