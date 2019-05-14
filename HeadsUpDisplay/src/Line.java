
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @author paul
 */
public class Line extends Segment 
{    
    public Line(MyPoint2D start, MyPoint2D end) {
        super(start, end);
    }

    /***
    @Override
    public double closestDistTo(double x, double y) {
        // twice the area of the triangle divided by the line length
        double dx = mEnd.x - mStart.x ;
        double dy = mEnd.y - mStart.y ;
        double lineLength = Math.sqrt(dx*dx+dy*dy) ;
        double numerator = dy*x - dx*y 
                           + mEnd.x*mStart.y 
                           - mEnd.y*mStart.x ;
        // let this be signed
        double dist = numerator / lineLength ;
        return dist ;
    }
    ***/

    @Override
    public void draw(GraphicsContext gc) {
        gc.setLineWidth(1.0/12.0);
        gc.setStroke(Color.PURPLE);
        
        double strtx = mStart.x ;
        double endx = mEnd.x ;

        // assume translation and scaling have been done by caller        
        gc.strokeLine(strtx, mStart.y, endx, mEnd.y) ;
    }

    @Override
    public Segment convertToFieldCoords(
            double targFieldX, double targFieldY, 
            double targAbsOrientCCW) {
        
        // first rotate back to field
        double theta = targAbsOrientCCW*Math.PI/180.0 ;
        
        double cosTheta = Math.cos(theta) ;
        double sinTheta = Math.sin(theta) ;
        
        double strtX = mStart.x ;
        double strtY = mStart.y ;
        double endX = mEnd.x ;
        double endY = mEnd.y ;
        
        double strtXrot = strtX*cosTheta - strtY*sinTheta ;
        double strtYrot = strtX*sinTheta + strtY*cosTheta ;
        double endXrot = endX*cosTheta - endY*sinTheta ;
        double endYrot = endX*sinTheta + endY*cosTheta ;
        
        // now translate to field origin
        strtX = strtXrot + targFieldX ;
        strtY = strtYrot + targFieldY ;
        endX = endXrot + targFieldX ;
        endY = endYrot + targFieldY ;
        
        // turn those into a new segment
        MyPoint2D sPt = new MyPoint2D(strtX, strtY) ;
        MyPoint2D ePt = new MyPoint2D(endX, endY) ;
        
        return new Line(sPt, ePt) ;
    }
    
    @Override
    public double getCurvature() {
        return 0 ;
    }
    
    @Override 
    public double getRadius() {
        return (Double.POSITIVE_INFINITY) ;
    }    
    
    @Override
    public int getDirection() {
        return 0 ;
    }
    
    @Override
    public String toString() {
        String str = String.format("Line from (%g,%g) to (%g,%g)", 
               mStart.x, mStart.y, mEnd.x, mEnd.y); 
        return str ;
    }
}
