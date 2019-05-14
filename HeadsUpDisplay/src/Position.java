
/**
 * @author Paul
 */
public class Position {
    public double x ;
    public double y ;
    public double orientCCW ;
    
    public Position() {}
    
    public Position(Position posn) {
        x = posn.x ;
        y = posn.y ;
        orientCCW = posn.orientCCW ;
    }

    public Position(double x, double y, double orient) {
        this.x = x ;
        this.y = y ;
        this.orientCCW = orient ;
    }
}
