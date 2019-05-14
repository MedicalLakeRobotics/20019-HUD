
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * @author paul
 */
public class Arc extends Segment 
{    
    private MyPoint2D mCenter ;
    private double mRadius ;
    private  int mDir ;  // + => CW, - => CCW
    
    public Arc(MyPoint2D center, double radius, MyPoint2D start,
               MyPoint2D end, int dir) {
        super(start, end);
        mCenter = center ;
        mRadius = radius ;
        mDir = dir ;
    }

    /***
    @Override
    public double closestDistTo(double x, double y) {
        double dx = (x - mCenter.x) ;
        double dy = (y - mCenter.y) ;
        double distToCtr = Math.sqrt(dx*dx + dy*dy) ;
        return (distToCtr - mRadius) ;
    }
    ***/
    
    @Override
    public void makeRelativeTo(double x, double y) {
        super.makeRelativeTo(x, y);
        mCenter.x -=x ;
        mCenter.y -=y ;
    }

    @Override
    public void draw(GraphicsContext gc) {          
        double strtx = mStart.x ;
        double endx = mEnd.x ;
        double ctrx = mCenter.x ;

        // assume translation and scaling have been done by caller
        double dxStrt = strtx - ctrx ;
        double dyStrt = mStart.y - mCenter.y ;
        double dxEnd = endx - ctrx ;
        double dyEnd = mEnd.y - mCenter.y ;
                
        double width = 2*mRadius ;
        double height = 2*mRadius ;
        
        double thetaStrt = Math.atan2(dyStrt, dxStrt) * 180.0 / Math.PI ; ;
        double thetaEnd = Math.atan2(dyEnd, dxEnd) * 180.0 / Math.PI ;
        // System.out.printf("Theta Start=%g Theta End=%g\n", thetaStrt, thetaEnd);
        
        double sweep = thetaEnd - thetaStrt ;
        if (sweep<-180) sweep += 360 ;
        if (sweep>180) sweep -= 360 ;

        double llx = ctrx-mRadius ;
        double lly = mCenter.y-mRadius ;
        
        /***
        double RAD = 2.0/12.0 ;
        gc.setFill(Color.GREEN);
        gc.fillOval(strtx-RAD, mStart.y-RAD, 2*RAD, 2*RAD); 
        gc.setFill(Color.RED);
        gc.fillOval(endx-RAD, mEnd.y-RAD, 2*RAD, 2*RAD); 
        gc.setFill(Color.BLUE);
        gc.fillOval(ctrx-RAD, mCenter.y-RAD, 2*RAD, 2*RAD); 
        ***/

        //System.out.printf("LL=(%g,%g) (ctr=%g,%g) Strt=%g stop=%g sweep=%g\n", 
        //        llx, lly, ctrx, mCenter.y, thetaStrt, thetaEnd, sweep);
        gc.setLineWidth(1.0/12.0);
        gc.setStroke(Color.MAGENTA);
        gc.strokeArc(llx, lly, width, height, -thetaStrt, -sweep, ArcType.OPEN) ;

        /*
        double ulx = -5 ;
        double uly = 5 ;
        width = 10 ;
        height = 10 ;
        sweep = 45 ;
        gc.setLineWidth(2.0/12.0);
        gc.setStroke(Color.RED);
        gc.strokeArc(ulx, uly, width, height, 0, -sweep, ArcType.OPEN) ;
        gc.setStroke(Color.GREEN);
        gc.strokeArc(ulx, uly, width, height, -45, -sweep, ArcType.OPEN) ;
        gc.setStroke(Color.BLUE);
        gc.strokeArc(ulx, uly, width, height, -90, -sweep, ArcType.OPEN) ;
        gc.setStroke(Color.BLACK);
        gc.strokeArc(ulx, uly, width, height, -135, -sweep, ArcType.OPEN) ;
        gc.strokeRect(ulx, uly, width, height) ;
        */
    }

    @Override
    public Segment convertToFieldCoords(
            double targFieldX, double targFieldY, 
            double targAbsOrientCCW) {
        
        // first rotate back to field
        double theta = targAbsOrientCCW*Math.PI/180.0 ;
        // if (targFieldX<0)  theta = -theta ;
        
        double cosTheta = Math.cos(theta) ;
        double sinTheta = Math.sin(theta) ;
        
        double strtX = mStart.x ;
        double strtY = mStart.y ;
        double endX = mEnd.x ;
        double endY = mEnd.y ;
        double ctrX = mCenter.x ;
        double ctrY = mCenter.y ;
        
        double strtXrot = strtX*cosTheta - strtY*sinTheta ;
        double strtYrot = strtX*sinTheta + strtY*cosTheta ;
        double endXrot = endX*cosTheta - endY*sinTheta ;
        double endYrot = endX*sinTheta + endY*cosTheta ;
        double ctrXrot = ctrX*cosTheta - ctrY*sinTheta ;
        double ctrYrot = ctrX*sinTheta + ctrY*cosTheta ;
        
        // now translate to field origin
        strtX = strtXrot + targFieldX ;
        strtY = strtYrot + targFieldY ;
        endX = endXrot + targFieldX ;
        endY = endYrot + targFieldY ;
        ctrX = ctrXrot + targFieldX ;
        ctrY = ctrYrot + targFieldY ;
        
        // turn those into a new segment
        MyPoint2D sPt = new MyPoint2D(strtX, strtY) ;
        MyPoint2D ePt = new MyPoint2D(endX, endY) ;
        MyPoint2D cPt = new MyPoint2D(ctrX, ctrY) ;
        
        return new Arc(cPt, mRadius, sPt, ePt, mDir) ;        
    }
    
    @Override 
    public double getCurvature() {
        return (1.0/mRadius) ;
    }
    
    @Override 
    public double getRadius() {
        return (mRadius) ;
    }
    
    @Override
    public int getDirection() {
        return mDir ;
    }
    
    
    @Override
    public String toString() {
        String str = String.format("Arc from (%g,%g) to (%g,%g)"
                +" ctr at (%g,%g) rad=%g dir= %d", 
               mStart.x, mStart.y, mEnd.x, mEnd.y,
               mCenter.x, mCenter.y, mRadius, mDir); 
        return str ;
    }    
}
