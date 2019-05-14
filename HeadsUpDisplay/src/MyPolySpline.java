
import java.io.Serializable;
import java.util.ArrayList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @author paul
 */
public class MyPolySpline implements Serializable
{
    static final double STEP = 0.1 ;
    static final double DOT_RAD_TENTHS_IN = 4 ;
    
    ArrayList<MyPoint2D> mPts = new ArrayList<>() ;
    //ArrayList<Point2D> mSelects = new ArrayList<>() ;
    
    public MyPolySpline() {       
    }
    
    public void add(MyPoint2D pt) {
        mPts.add(pt) ;
    }
    
    public void drawAsPoly(GraphicsContext g, double lineWidth) {
        if (mPts.size()<3)  return ;

        g.save();
        g.setLineWidth(lineWidth);
        
        g.beginPath();
        g.moveTo(mPts.get(0).x, mPts.get(0).y);
        for (int i=1 ; i<mPts.size() ; i++) {
            g.lineTo(mPts.get(i).x, mPts.get(i).y);
        }
        //g.closePath();
        g.stroke();
        
        g.restore();
    }
    
    public void drawAsSpline(GraphicsContext g, double lineWidth) {
        int ii;
        double tt, tt2, tt3, x3, x2, x1, x0, y3, y2, y1, y0, x, y;
        
        if (mPts.size()<3)  return ;
        
        g.save();
        g.setLineWidth(lineWidth);
        
        g.beginPath();
        g.moveTo(mPts.get(0).x, mPts.get(0).y);

        // start at pt[0]
        x3 = 2 * mPts.get(0).x - 3 * mPts.get(1).x + mPts.get(2).x;
        y3 = 2 * mPts.get(0).y - 3 * mPts.get(1).y + mPts.get(2).y;
        x2 = -3 * mPts.get(0).x + 3 * mPts.get(1).x;
        y2 = -3 * mPts.get(0).y + 3 * mPts.get(1).y;
        x1 = x2;
        y1 = y2;
        x0 = 5 * mPts.get(0).x + mPts.get(1).x;
        y0 = 5 * mPts.get(0).y + mPts.get(1).y;
        for (tt = 0; tt <= 1; tt += STEP) {
            tt2 = tt * tt;
            tt3 = tt2 * tt;
            x = (tt3 * x3 + tt2 * x2 + tt * x1 + x0) / 6 + 0.5;
            y = (tt3 * y3 + tt2 * y2 + tt * y1 + y0) / 6 + 0.5;
            g.lineTo(x, y);
        }

        // pts[1] thru n-3 
        for (ii = 1; ii < mPts.size() - 2; ii++) {
            x3 = -mPts.get(ii - 1).x + 3 * mPts.get(ii).x - 3 * mPts.get(ii + 1).x + mPts.get(ii + 2).x;
            y3 = -mPts.get(ii - 1).y + 3 * mPts.get(ii).y - 3 * mPts.get(ii + 1).y + mPts.get(ii + 2).y;
            x2 = 3 * mPts.get(ii - 1).x - 6 * mPts.get(ii).x + 3 * mPts.get(ii + 1).x;
            y2 = 3 * mPts.get(ii - 1).y - 6 * mPts.get(ii).y + 3 * mPts.get(ii + 1).y;
            x1 = -3 * mPts.get(ii - 1).x + 3 * mPts.get(ii + 1).x;
            y1 = -3 * mPts.get(ii - 1).y + 3 * mPts.get(ii + 1).y;
            x0 = mPts.get(ii - 1).x + 4 * mPts.get(ii).x + mPts.get(ii + 1).x;
            y0 = mPts.get(ii - 1).y + 4 * mPts.get(ii).y + mPts.get(ii + 1).y;

            for (tt = 0; tt <= 1; tt += STEP) {
                tt2 = tt * tt;
                tt3 = tt2 * tt;
                x = (tt3 * x3 + tt2 * x2 + tt * x1 + x0) / 6 + 0.5;
                y = (tt3 * y3 + tt2 * y2 + tt * y1 + y0) / 6 + 0.5;
                g.lineTo(x, y);
            }
        }

        // pt[n-2]
        ii = mPts.size() - 2;
        x3 = -mPts.get(ii - 1).x + 3 * mPts.get(ii).x - 2 * mPts.get(ii + 1).x;
        y3 = -mPts.get(ii - 1).y + 3 * mPts.get(ii).y - 2 * mPts.get(ii + 1).y;
        x2 = 3 * mPts.get(ii - 1).x - 6 * mPts.get(ii).x + 3 * mPts.get(ii + 1).x;
        y2 = 3 * mPts.get(ii - 1).y - 6 * mPts.get(ii).y + 3 * mPts.get(ii + 1).y;
        x1 = -3 * mPts.get(ii - 1).x + 3 * mPts.get(ii + 1).x;
        y1 = -3 * mPts.get(ii - 1).y + 3 * mPts.get(ii + 1).y;
        x0 = mPts.get(ii - 1).x + 4 * mPts.get(ii).x + mPts.get(ii + 1).x;
        y0 = mPts.get(ii - 1).y + 4 * mPts.get(ii).y + mPts.get(ii + 1).y;
        for (tt = 0; tt <= 1; tt += STEP) {
            tt2 = tt * tt;
            tt3 = tt2 * tt;
            x = (tt3 * x3 + tt2 * x2 + tt * x1 + x0) / 6 + 0.5;
            y = (tt3 * y3 + tt2 * y2 + tt * y1 + y0) / 6 + 0.5;
            g.lineTo(x, y) ;
        }

        // pt[n-1]
        ii = mPts.size() - 1;
        x3 = -mPts.get(ii - 1).x + mPts.get(ii).x;
        y3 = -mPts.get(ii - 1).y + mPts.get(ii).y;
        x2 = 3 * mPts.get(ii - 1).x - 3 * mPts.get(ii).x;
        y2 = 3 * mPts.get(ii - 1).y - 3 * mPts.get(ii).y;
        x1 = -3 * mPts.get(ii - 1).x + 3 * mPts.get(ii).x;
        y1 = -3 * mPts.get(ii - 1).y + 3 * mPts.get(ii).y;
        x0 = mPts.get(ii - 1).x + 5 * mPts.get(ii).x;
        y0 = mPts.get(ii - 1).y + 5 * mPts.get(ii).y;
        for (tt = 0; tt <= 1; tt += STEP) {
            tt2 = tt * tt;
            tt3 = tt2 * tt;
            x = (tt3 * x3 + tt2 * x2 + tt * x1 + x0) / 6 + 0.5;
            y = (tt3 * y3 + tt2 * y2 + tt * y1 + y0) / 6 + 0.5;
            g.lineTo(x, y);
        }        

        //g.closePath();
        g.stroke();
        
        g.restore();
    }

    void drawSelect(GraphicsContext g) {
        MyPoint2D vp;
        g.setFill(Color.BLUE);
        double dotrad = DOT_RAD_TENTHS_IN;
        for (int ii = 0; ii < mPts.size(); ii++) {
            vp = mPts.get(ii);
            g.fillRect(vp.x - dotrad, vp.y - dotrad,
                    2 * dotrad, 2 * dotrad);
        }
    }     

    int whichVertex(MyPoint2D pt) {        
   	double dotrad = DOT_RAD_TENTHS_IN ;
        for (int ii=0 ; ii<mPts.size() ; ii++) {
            // check pt
            if (     (mPts.get(ii).x <= (pt.x + dotrad))
		  && (mPts.get(ii).x >= (pt.x - dotrad))
		  && (mPts.get(ii).y <= (pt.y + dotrad))
		  && (mPts.get(ii).y >= (pt.y - dotrad)) 
                ) 
            {
                return ii ;
            }
            /***
            // System.out.printf("checking pt %d (%g,%g) against (%g,%g),(%g,%g) failed\n",
                    ii, mPts.get(ii).x, mPts.get(ii).y,
                    pt.x-dotrad, pt.x+dotrad,
                    pt.y-dotrad, pt.y+dotrad);
            ***/
        }
        return -1 ;
    }
} 
