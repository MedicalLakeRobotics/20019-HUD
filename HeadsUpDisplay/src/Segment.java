
import javafx.scene.canvas.GraphicsContext;

/*
 * @author paul schimpf
 */
public abstract class Segment 
{
    protected MyPoint2D mStart, mEnd ;
    
    public Segment(MyPoint2D start, MyPoint2D end) {
        mStart = start ;
        mEnd = end ;
    }
    
    public MyPoint2D getStart() {
        return mStart ;
    }
    
    public MyPoint2D getEnd() {
        return mEnd ;
    }
    
    /***
    MyPoint2D ftLocToPixLoc(double ftX, double ftY, double pixPerIn,
            double pixWinW, double pixWinH) {

        // scale
        double pixX = ftX * 12.0 * pixPerIn ;
        double pixY = ftY * 12.0 * pixPerIn ;
        
        // translate relative to graphics origin at upper left
        pixX = pixX + pixWinW/2.0 ; ;
        pixY = pixWinH - pixY ; 
        
        return new MyPoint2D(pixX, pixY) ;
    }
    
    public boolean isPastEnd(double x, double y, double headDegCCW) {
        double dx = x - mEnd.x ;
        double dy = y - mEnd.y ;
        double phiCCW = Math.atan2(dy, dx);
        double bearingCW = headDegCCW - phiCCW*180.0/Math.PI ;
        boolean past = (Math.abs(bearingCW)>=90.0) ;
        return past ;
    }
    
    public double endDistTo(double x, double y) {
        double dx = x - mEnd.x ;
        double dy = y - mEnd.y ;
        return Math.sqrt(dx*dx+dy*dy) ;
    }

    abstract public double closestDistTo(double x, double y);
    ****/

    public void makeRelativeTo(double x, double y) {
        mStart.x -= x ;
        mStart.y -= y ;
    }
    
    abstract public int getDirection() ;
    
    abstract public double getCurvature() ;

    abstract public double getRadius() ;
    
    // below here are only necessary for simulation
    abstract public Segment convertToFieldCoords(
            double targFieldX, double targFieldY, 
            double targAbsOrientCCW) ;

    abstract public void draw(GraphicsContext gc) ; 
}
