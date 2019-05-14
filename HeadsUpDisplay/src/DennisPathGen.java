import java.util.ArrayList;

/**
 * @author dennis in python, ported to java by paul
 * implemented as a Singleton because only one should ever exist, and 
 * segments as a static field from the animation class isn't really kosher
 */
public final class DennisPathGen
{
    private static final DennisPathGen INSTANCE =  new DennisPathGen() ;
    
    // this controls how sharp the pathgen turns are
    private static final double BASE_MIN_TURN_RADIUS = 1.0 ; // 1.5 ; // ft
    
    // public so the animator can access it
    // TODO: fix that by surrounding it with a get method
    public ArrayList<Segment> mSegments = new ArrayList<Segment>() ;
    int mCurrSeg = 0 ;
    
    // location of robot and its sensor
    // in the target frame of reference
    double mBotNormOrientCCW ;
    double mBotRelXnorm, mBotRelYnorm ;
    
    // target standoff
    double mTargetStandoff ;

    // constructor
    private DennisPathGen() {}
    
    public static DennisPathGen getInstance() {
        return INSTANCE ;
    }

    // takes the place of a constuctor
    public void init(double targetStandoff) {
        mTargetStandoff = targetStandoff ;
    }
    
    public ArrayList<Segment> pathUpdate(Position botNormPos) 
                                        throws NoPathException {
        mSegments.clear();
        mCurrSeg = 0 ;
        
        mBotRelXnorm = botNormPos.x ;
        mBotRelYnorm = botNormPos.y ;
        mBotNormOrientCCW = botNormPos.orientCCW ;
       
        boolean success = trySingleSolution() ;
        if (!success)
            success = tryDoubleSolution() ;
        if (!success) {
            throw new NoPathException("") ;
        }
        
        /*
        for (Segment seg : mSegments) {
            System.out.println("GEN: "+seg);
        }
        */
       
        return mSegments ;
    }

    private boolean trySingleSolution() {
        mSegments.clear();
        
        int quad = getQuadRelToTarg();
        
        if ((quad==3) && (mBotRelYnorm<0))
            return false ;
        if ((quad==4) && (mBotRelYnorm<0))
            return false ;
        if ((quad==1) && (mBotRelYnorm>0))
            return false ;
        if ((quad==2) && (mBotRelYnorm>0))
            return false ;
        
        MyPoint2D startPt = new MyPoint2D(mBotRelXnorm, mBotRelYnorm) ;
        MyPoint2D endPt = new MyPoint2D(mTargetStandoff, 0) ;
        
        double orientRad = mBotNormOrientCCW * Math.PI / 180.0 ;
        double interceptX = startPt.x - (startPt.y 
                        * Math.sin(Math.PI/2.0-orientRad) / Math.sin(orientRad)) ;
        if (interceptX < mTargetStandoff)
            return false ;

        MyPoint2D xAxisInterceptPt = new MyPoint2D(interceptX, 0) ;
        double distX = Math.abs(interceptX-mTargetStandoff) ;
        // this is actually the hypotenuse
        double distY = startPt.distTo(xAxisInterceptPt) ;
        double radius ;
        if (distX >= distY) 
            radius = calcTurnRadius(distY, orientRad+Math.PI) ;
        else 
            radius = calcTurnRadius(distX, orientRad+Math.PI) ;
        
        if (radius<BASE_MIN_TURN_RADIUS)
            return false ;
        
        // NOTE: tan(theta+180) = tan(theta)
        double sideDist = radius*Math.tan((orientRad+Math.PI)/2.0);
        //System.out.printf("distx=%g disty=%g radius=%g sideDist=%g\n", 
        //                    distX, distY, radius, sideDist);
        
        MyPoint2D arcPt1, arcPt2 ;
        if (distX >= distY) {
            // arc starts at vehicle
            arcPt1 = startPt ;
            arcPt2 = new MyPoint2D(interceptX - Math.abs(sideDist), 0) ;
        }
        else {
            // arc ends at target
            arcPt1 = new MyPoint2D(interceptX 
                    + Math.abs(sideDist)*Math.cos(orientRad+Math.PI),
                    Math.abs(sideDist)*Math.sin(orientRad+Math.PI)) ;
            arcPt2 = endPt ;
            //System.out.printf("ArcStart=%s\n",arcPt1);
            //System.out.println("ArcEnd="+endPt);
        }
        
        // possible negative radius?
        MyPoint2D centerPt = new MyPoint2D(arcPt2.x, 
                                   Math.copySign(radius, sideDist)) ;
        // System.out.printf("Pt1=%s Pt2=%s\n", arcPt1, arcPt2) ;
        
        Segment arcSeg ;
        if ((centerPt.y>0))
            arcSeg = new Arc(centerPt, radius, arcPt1, arcPt2, +1) ;
        else
            arcSeg = new Arc(centerPt, radius, arcPt1, arcPt2, -1) ;
        
        // NOTE: comparing refs is OK here
        if (arcPt1==startPt) {
            mSegments.add(arcSeg) ;
            mSegments.add(new Line(arcPt2, endPt)) ;
        }
        else {
            Line line = new Line(startPt, arcPt1) ;
            mSegments.add(line) ;
            mSegments.add(arcSeg );
            // System.out.println("PathGen Seg 1 ="+line) ;
            // System.out.println("PathGen Seg 2 ="+arcSeg) ;
        }            
        return true ;
    }

    private boolean tryDoubleSolution() {
        mSegments.clear();
        MyPoint2D startPt = new MyPoint2D(mBotRelXnorm, mBotRelYnorm) ;
        MyPoint2D endPt = new MyPoint2D(mTargetStandoff, 0) ;
        
        double orientRad = mBotNormOrientCCW * Math.PI / 180.0 ;
        double interceptX = startPt.x - (startPt.y 
                        * Math.sin(Math.PI-orientRad) / Math.sin(orientRad)) ;

        MyPoint2D xAxisInterceptPt = new MyPoint2D(interceptX, 0) ;
        double distX = Math.abs(interceptX-mTargetStandoff) ;
        // TODO: this is actually the hypotenuse?
        double distY = startPt.distTo(xAxisInterceptPt) ;
        //System.out.printf("distx=%g disty=%g interceptX=%g\n", 
        //                    distX, distY, interceptX);

        DblCircStats stats = calcDblCirc(startPt, endPt, mBotNormOrientCCW,
                                 distX, distY) ;
        if (stats.radius<BASE_MIN_TURN_RADIUS)
            return false ;

        MyPoint2D arc1Pt1 = startPt ;
        MyPoint2D arc1Pt2 = new MyPoint2D((stats.ctr2.x+stats.ctr1.x)/2.0,
                                          (stats.ctr2.y+stats.ctr1.y)/2.0) ;
        //System.out.printf("Pt1a=%s Pt2a=%s\n", arc1Pt1, arc1Pt2) ;

        MyPoint2D arc2Pt1 = arc1Pt2 ;
        MyPoint2D arc2Pt2 = endPt ;
        //System.out.printf("Pt1b=%s Pt2b=%s\n", arc2Pt1, arc2Pt2) ;
        
        int dir1 = stats.arc1Dir ;
        int dir2 = stats.arc2Dir ;
        double radius = stats.radius ;
        if (radius<0) {
            dir1 = -dir1 ;
            dir2 = -dir2 ;
            radius = -radius ;
        }
        Segment arcSeg1 = new Arc(stats.ctr1, stats.radius, arc1Pt1, arc1Pt2, dir1) ;    
        Segment arcSeg2 = new Arc(stats.ctr2, stats.radius, arc2Pt1, arc2Pt2, dir2) ;     
        mSegments.add(arcSeg1) ;
        mSegments.add(arcSeg2) ;
        
        return true ;
    }

    private DblCircStats calcDblCirc(MyPoint2D carPt, MyPoint2D endTargetPt, 
                              double angleDeg, double distX, double distY) {
        DblCircStats stats = new DblCircStats() ;
        
        AngleQuadStats AQstats = angleQuad(carPt, distX, distY) ;
        double angleRad = angleDeg * Math.PI / 180.0 ;
               
        double carx = carPt.x - endTargetPt.x ;
        double cary = carPt.y - endTargetPt.y ;
        double tAngle = Math.abs(angleRad - Math.PI/2.0) ;
        double a = (2 - AQstats.ctr2sign*2*AQstats.ctr1ysign*Math.sin(tAngle));
        double b = (2 * (carx*AQstats.ctr1xsign*Math.cos(tAngle))
                      + AQstats.ctr2sign*2*cary 
                      + AQstats.ctr1ysign*2*cary*Math.sin(tAngle)) ;
        double c = -1 * (carx*carx + cary*cary) ;
        stats.radius = quadratic(a,b,c) ;
        stats.ctr2 = new MyPoint2D(0, AQstats.ctr2sign*stats.radius) ;
        stats.ctr1 = new MyPoint2D(carx-stats.radius*AQstats.ctr1xsign*Math.cos(tAngle), 
                          cary-stats.radius*AQstats.ctr1ysign*Math.sin(tAngle)) ;       
        stats.ctr2.x = (stats.ctr2.x + endTargetPt.x) ;
        stats.ctr2.y = (stats.ctr2.y + endTargetPt.y) ;
        stats.ctr1.x = (stats.ctr1.x + endTargetPt.x) ;
        stats.ctr1.y = (stats.ctr1.y + endTargetPt.y) ;
        // ctr2Sign indicates if the second Arc segment center is above the Xaxis 
        // (+ is above Xaxis - is below Xaxis)
        // On a Dble curve solution, if the second Arc ctr2sign>0 (above Xaxis) 
        // the Arc curve will be CW. The first Arc curve will always be 
        // opposite the second arc curve. arcXDir (+ is CW, -is CCW) 
        stats.arc2Dir = AQstats.ctr2sign ;
        stats.arc1Dir = AQstats.ctr2sign * -1 ;
        return stats ;
    }
    
    private double calcTurnRadius(double hyp, double orientRadCCW) {
        double radius = hyp / Math.tan(orientRadCCW/2.0) ;
        return Math.abs(radius) ;
    }
    
    private double quadratic(double a, double b, double c) {
        if (a==0) a=.0000000001 ;
        double part2 = b*b - 4*a*c ;
        if (part2<=0)
            System.out.println("*** SQUARE ROOT OF NEGATIVE ***");
        double x1 = (-b + Math.sqrt(part2))/(2*a) ;
        double x2 = (-b - Math.sqrt(part2))/(2*a) ;
        // System.out.printf("x1=%g x2=%g\n", x1, x2) ;
        if (Math.abs(x1) < Math.abs(x2))
            return x1 ;
        return x2 ;        
    }
    
    private int getQuadRelToTarg() {
        double orient = mBotNormOrientCCW ;
        if (orient<0) orient += 360 ;
        if (orient>360) orient -= 360 ;
        if (orient>0 && orient<=90) 
            return 1 ;
        if (orient>90 && orient<=180) 
            return 2 ;
        if (orient>180 && orient<=270) 
            return 3 ;
        return 4 ;
    }
    
    private AngleQuadStats angleQuad(MyPoint2D start, 
                           double distx, double disty) {
    
        AngleQuadStats stats = new AngleQuadStats() ;
        
        // this came from a Python list
        int[][] signArray2 = { {0,+1,+1,+1},
                               {1,-1,-1,-1},
                               {2,+1,+1,+1},
                               {3,-1,-1,+1},
                               {4,+1,+1,+1},
                               {5,+1,+1,-1},
                               {6,-1,-1,-1},
                               {7,+1,+1,+1},
                               {8,-1,-1,-1},
                               {9,+1,+1,+1}} ;
        
        int quad = getQuadRelToTarg() ;
        if (start.y <= 0) {
            if      (quad == 3)   stats.flag = 1;
            else if (quad == 4)   stats.flag = 2 ;
            else if ((quad == 1) && (distx >= disty)) stats.flag = 5;
            else if ((quad == 1) && (distx < disty))  stats.flag = 6;
            else if ((quad == 2) && (distx < disty))  stats.flag = 6;
            else if ((quad == 2) && (distx >= disty)) stats.flag = 7;      
        }
        else {
            if      (quad == 1)   stats.flag = 3;
            else if (quad == 2)   stats.flag = 4;
            else if ((quad == 3) && (distx >=disty)) stats.flag = 8;
            else if ((quad == 4) && (distx >=disty)) stats.flag = 8;
            else if ((quad == 3) && (distx < disty)) stats.flag = 9;
            else if ((quad == 4) && (distx < disty)) stats.flag = 9;            
        }

        stats.arc2Dir   = signArray2[stats.flag][1] ;
        stats.arc1Dir   = (signArray2[stats.flag][1]) * -1 ;
        stats.ctr2sign  = signArray2[stats.flag][1] ;
        stats.ctr1xsign = signArray2[stats.flag][2] ;
        stats.ctr1ysign = signArray2[stats.flag][3] ;  

        return stats ;
    }
    
    private class DblCircStats {
        public double radius ; 
        public MyPoint2D ctr1 ;
        public MyPoint2D ctr2 ;
        public int arc2Dir ;
        public int arc1Dir ;
    }
    
    // arc1dir is actually for arc2 and visa versa, but
    // not messing with it because Dennis says it works
    private class AngleQuadStats {
        public int flag ;               // barf
        public int ctr2sign ;
        public int ctr1xsign ;
        public int ctr1ysign ;
        public int arc2Dir ;
        public int arc1Dir ;
    }
    
    public void printSegments() {
        for (int i=0 ; i<mSegments.size() ; i++) {
            Segment seg = mSegments.get(i) ;
            System.out.printf("Seg %d: start=(%g,%g) end=(%g,%g)\n", i,
                    seg.mStart.x, seg.mStart.y, 
                    seg.mEnd.x, seg.mEnd.y);
            /***
            Segment segField = seg.convertToFieldCoords(
                    absTargX, mAbsTargY, targetOrient) ;
            ***/
        }        
    }

    @Override
    public String toString() {
        String str = String.format(
          "TARGET FRAME: Robot at (%g,%g) Robot Orient = %g",
          mBotRelXnorm, mBotRelYnorm, mBotNormOrientCCW);
        return str ;
    }  
}
