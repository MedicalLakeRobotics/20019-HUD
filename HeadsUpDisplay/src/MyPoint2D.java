

import java.io.IOException;
import java.io.Serializable;
import javafx.geometry.Point2D;

/*
 * @author paul
 */
public class MyPoint2D implements Serializable {

    public double x;
    public double y;

    public MyPoint2D(double x, double y) {
        this.x = x ;
        this.y = y ;
    }

    public Point2D getPoint() {
        return new Point2D(x, y);
    }

    public double distTo(MyPoint2D dest) {
        double dx = dest.x - x ;
        double dy = dest.y - y ;
        return Math.sqrt(dx*dx+dy*dy) ;
    }
    
    @Override public String toString() {
        String str = String.format("(%g, %g)", x, y) ;
        return str ;
    }
}
