
import java.util.ArrayList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/*
 * @author paul
 */
public class HeadsUpDisplay extends Region 
{
    // drawing params
    private static final double ROBOT_WIDTH = 2.75;   // ft
    private static final double ROBOT_LENGTH = 3.17;  // ft

    // preferred startup size, prior to loading an image or configuration
    private static final double PREF_PIX_WIDTH = 800;
    private static final double PREF_PIX_HEIGHT = 800;
    private static final double LINE_WIDTH_IN = 4 ;
    private static final double ZOOM_MARGIN_FT = 2.5;

    // Main class for status callback TODO: make an interface
    private Main mMain ;
    private boolean mStatusInFt = true ;
    
    // layers to draw on
    private Canvas mFieldCanvas = new Canvas();  // for the background
    private boolean mDrawImage ;        // whether to draw background
    private Canvas mPathCanvas = new Canvas();
    private ArrayList<Segment> mSegs = new ArrayList<>(); 
    // two messages at upper left and upper right
    private String mMsg1, mMsg2 ;
    
    private Image mImage;                   // the field image
    private double mInvertMultiplier = 1 ;  // for reflection
    private double mAspectRatio ; 
    private double mImageScale ;        // ppi for the image
    private double mUpScale;            // upscale from image to window
    private double mScale ;             // final scale w/o zoom (scrn pix/in)
    
    // zoom stuff
    private boolean mDoAutoZoom;         // whether to auto zoom
    private boolean mIsZoomed = false;   // when above is true and Path defined
    private double mZoomScale = 1.0 ;    // or user does manually
    private double mZoomPixULx, mZoomPixULy ;
    private double mZoomWidthPix, mZoomHeightPix ;
    
    private Position mBotPosn = new Position() ;

    // coordinate system origin in image pixs (now defined by user)
    private double mOriginImagePixY;
    private double mOriginImagePixX;
    // coordinate system origin in screen pixs
    private double mOriginScrnPixY ;
    private double mOriginScrnPixX ;

    public HeadsUpDisplay(Main main) {
        setPrefWidth(PREF_PIX_WIDTH);
        setPrefHeight(PREF_PIX_HEIGHT);
        getChildren().addAll(mFieldCanvas, mPathCanvas);
        
        mMain = main ;
        mFieldCanvas.addEventHandler(MouseEvent.MOUSE_MOVED,
                event -> onMouseMoved(event));
        mPathCanvas.addEventHandler(MouseEvent.MOUSE_MOVED,
                event -> onMouseMoved(event));
        mFieldCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> onMousePressed(event));
        mPathCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> onMousePressed(event));
        setStyle("-fx-background-color: darkgray;") ;
    }
    
    public void setSettings(boolean bkgrnd, Image img, double ppi, double origX, 
                    double origY, boolean autoZoom, double mirrorFactor) {
        mImage = img ;
        mAspectRatio = mImage.getWidth() / mImage.getHeight() ;

        mOriginImagePixX = origX ;
        mOriginImagePixY = origY ;
        mImageScale = ppi* 12.0 ;
        mDrawImage = bkgrnd ;
        mDoAutoZoom = autoZoom;
        mIsZoomed = false ;
        mInvertMultiplier = mirrorFactor ;
        layoutChildren();
    }
    
    public void setImage(Image img) {
        mImage = img ;
        draw() ;
    }

    public void setPositions(Position botPosn) {
        mBotPosn = botPosn ;
    }
    
    public void setSegments(ArrayList<Segment> fieldSegs) {
        unZoom() ;
        mSegs = fieldSegs ;
    }    

    public void setMessages(String msg1, String msg2) {
        mMsg1 = msg1 ;
        mMsg2 = msg2 ;
    }
       
    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        double contentWidth = getWidth();
        double contentHeight = getHeight();

        double calcWidth = contentHeight * mAspectRatio;
        double calcHeight = contentWidth / mAspectRatio;

        double finalWidth, finalHeight;

        if (calcHeight > contentHeight) {
            finalWidth = calcWidth;
            finalHeight = contentHeight;
        } else {
            finalWidth = contentWidth;
            finalHeight = calcHeight;
        }

        mPathCanvas.setWidth(finalWidth);
        mPathCanvas.setHeight(finalHeight);
        mFieldCanvas.setWidth(finalWidth);
        mFieldCanvas.setHeight(finalHeight);

        // center the Canvas
        layoutInArea(mFieldCanvas, 0, 0, contentWidth, contentHeight, 0,
                HPos.CENTER, VPos.CENTER);
        layoutInArea(mPathCanvas, 0, 0, contentWidth, contentHeight, 0,
                HPos.CENTER, VPos.CENTER);
        
        // scale goes up and down as window grows and shrinks
        // we can use either dimension for this since the aspect
        // ratio is being held constant
        mUpScale = 1.0 ;
        if (mImage!=null)
            mUpScale = finalWidth / mImage.getWidth() ;
        
        mScale = mImageScale * mUpScale ;
        
        // adjust the origin based on the "upscale" to screen size
        // (and mirror imaging)
        double origImagePixY ;
        if (mInvertMultiplier>0)
            origImagePixY = mOriginImagePixY ;
        else
            origImagePixY = mImage.getHeight() - mOriginImagePixY ;
        mOriginScrnPixX = mOriginImagePixX * mUpScale ;
        mOriginScrnPixY = origImagePixY * mUpScale ;
        
        draw();
    }
    
    private void calcZoom() {
        // search for extrema
        double xmin = Double.POSITIVE_INFINITY ;
        double xmax = Double.NEGATIVE_INFINITY ;
        double ymin = Double.POSITIVE_INFINITY ;
        double ymax = Double.NEGATIVE_INFINITY ;
        for (int i=0 ; i<mSegs.size() ; i++) {
            Segment seg = mSegs.get(i) ;
            MyPoint2D start = seg.getStart() ;
            MyPoint2D end = seg.getEnd() ;
            if (start.x<xmin) 
                xmin = start.x ;
            if (start.y<ymin) 
                ymin = start.y ;
            if (end.x<xmin) 
                xmin = end.x ;
            if (end.y<ymin) 
                ymin = end.y ;
            if (start.x>xmax) 
                xmax = start.x ;
            if (start.y>ymax) 
                ymax = start.y ;
            if (end.x>xmax) 
                xmax = end.x ;
            if (end.y>ymax) 
                ymax = end.y ;        
        }

        // add some margins and compute window dimensions in ft and pixels
        // (which are saved to member vars)
        computeZoomWindowDims(xmin, ymin, xmax, ymax) ;
    }
    
    // computes zoom window UL, width, height, and aspect ratio
    // return the dimensions in ft
    private void computeZoomWindowDims(double xmin, double ymin, 
                                            double xmax, double ymax) 
    {
        // seed the zoom window dimensions
        MyPoint2D ULft = new MyPoint2D(xmin, ymax) ;
        MyPoint2D LRft = new MyPoint2D(xmax, ymin) ;
        
        // Adding margins is easier to do in image pixels, because the
        // minima and maxima are easier. So do that first.
        // Get window above into pix and adjust for margins
        double marginPix = ZOOM_MARGIN_FT *mImageScale ;
        MyPoint2D ULpix = FtLocToPixLoc(ULft.x, ULft.y, 
                        mOriginImagePixX, mOriginImagePixY, mImageScale) ;
        MyPoint2D LRpix = FtLocToPixLoc(LRft.x, LRft.y, 
                        mOriginImagePixX, mOriginImagePixY, mImageScale) ;
        
        // adjust the window with margins
        ULpix.x = Math.max(0, ULpix.x-marginPix) ;
        ULpix.y = Math.max(0, ULpix.y-marginPix) ;
        LRpix.x = Math.min(LRpix.x+marginPix, mImage.getWidth()) ;
        LRpix.y = Math.min(LRpix.y+marginPix, mImage.getHeight()) ;

        // the aspect ratio of the zoomed area
        double zoomWidthPix = (LRpix.x-ULpix.x) ;
        double zoomHeightPix = (LRpix.y-ULpix.y) ;
        
        //mAspectRatio = mZoomWidthPix / mZoomHeightPix ;
        
        double zoom1 = mImage.getHeight() / zoomHeightPix ;
        double zoom2 = mImage.getWidth() / zoomWidthPix ;
        mZoomScale = Math.min(zoom1, zoom2) ;

        mZoomPixULx = ULpix.x ;
        mZoomPixULy = ULpix.y ;
        mZoomWidthPix = zoomWidthPix ;
        mZoomHeightPix = zoomHeightPix ;
    }

    // msg is a string to superimpose on the drawing
    public void draw() {
        if (mDrawImage) drawField() ;
        drawMsgs() ;
        drawPath(mBotPosn) ;
        if (mDoAutoZoom) {
            if (mIsZoomed)
                unZoom() ;
            mIsZoomed = true ;
            doZoom();
        }        
    }
    
    private void drawField() {
        GraphicsContext g = mFieldCanvas.getGraphicsContext2D();
        // draw the field image
        double canW = mFieldCanvas.getWidth();
        double canH = mFieldCanvas.getHeight();

        // zoom is bit tricky for the image, because it is drawn in 
        // src = image pix coords and dest = logical pix coords
        // can we draw the image post-scaling in ft?
        MyPoint2D srcUL ;
        double srcW, srcH ;
            srcUL = new MyPoint2D(0,0) ;
            srcW = mImage.getWidth() ;
            srcH = mImage.getHeight() ;
        double dstuly, dsthght ;
        if (mInvertMultiplier<0) {
            dstuly = canH ;
            dsthght = -canH ;            
        }
        else {
            dstuly = 0 ;
            dsthght = canH ;
        }        
        g.drawImage(mImage, srcUL.x, srcUL.y, srcW, srcH,
                  0f, dstuly, canW, dsthght);

        // draw a little origin
        g.setLineWidth(1);    
        // y axis
        g.setStroke(Color.RED);
        g.strokeLine(mOriginScrnPixX, mOriginScrnPixY, 
                mOriginScrnPixX, mOriginScrnPixY-20*mInvertMultiplier);
        // x axis
        g.setStroke(Color.BLUE);
        g.strokeLine(mOriginScrnPixX, mOriginScrnPixY, 
                mOriginScrnPixX+20, mOriginScrnPixY); 
    }
    
    private void drawMsgs() {
        GraphicsContext g = mFieldCanvas.getGraphicsContext2D();
        double w = mFieldCanvas.getWidth();
        double h = mFieldCanvas.getHeight();

        //g.clearRect(0, 0, w, h);
        g.setFill(Color.BLUE);
        g.setFont(new Font("Courier New Bold", 26));
        if (mMsg1!=null) {
            g.fillText(mMsg1, 10, 24);
        }
        if (mMsg2!=null) {
            g.fillText(mMsg2, w-17*mMsg2.length(), 24);
        }        
    }
    
    private void drawPath(Position botPosn) {
        GraphicsContext g = mPathCanvas.getGraphicsContext2D();
        double w = mPathCanvas.getWidth();
        double h = mPathCanvas.getHeight();
        
        g.clearRect(0, 0, w, h);
        g.save();
        // window ft to screen pixels
        g.translate(mOriginScrnPixX, mOriginScrnPixY);
        g.scale(mScale, -mScale*mInvertMultiplier);

        // draw segments
        for (int i=0 ; i<mSegs.size() ; i++) {
            Segment seg = mSegs.get(i) ;
            seg.draw(g) ;
        }
        
        // draw the robot
        drawRobot(g, botPosn.x, botPosn.y, botPosn.orientCCW) ;            

        g.restore();        
    }
    
    private void drawRobot(GraphicsContext g, 
            double botX, double botY, double botOrient) {
        g.save() ;
        // draw the robot as a rectangle
        g.translate(botX, botY);
        g.rotate(botOrient);
        g.setLineWidth(1.0/12.0);
        g.setStroke(Color.BLUE);
        g.strokeRect(-ROBOT_LENGTH/2.0, -ROBOT_WIDTH/2.0, ROBOT_LENGTH, ROBOT_WIDTH);
        
        // draw the robot direction triangle
        double triX[] = {-0.5, 0.5, -0.5} ; // {0.5, ROBOT_LENGTH/2.0, 0.5} ;
        double triY[] = {0.5, 0, -0.5} ;    // {ROBOT_WIDTH/4.0, 0, -ROBOT_WIDTH/4.0} ;
        
        g.setFill(Color.BLUE);
        g.fillPolygon(triX, triY, 3);
        g.restore();
    }

    private void onMouseMoved(MouseEvent event) {
        double xp = event.getX() ;
        double yp = event.getY() ;
        MyPoint2D pt ;
        pt = pixLocToFtLoc(xp, yp, mOriginScrnPixX,
                mOriginScrnPixY, mScale) ;
    
        String str ;
        if (mStatusInFt)
            str = String.format("loc = (%g, %g) ft", pt.x, pt.y) ;
        else
            str = String.format("loc scrn px = (%g,%g) ==> (%g,%g) img px", 
                    xp, yp, xp/mUpScale, yp/mUpScale) ;
        mMain.setStatus(str);        
    }

    private void onMousePressed(MouseEvent event) {
        mStatusInFt = !mStatusInFt ;
    }

    // we have two pix origins and scales (image and logical)
    // so pass in the ones you want to convert from
    MyPoint2D pixLocToFtLoc(double pixX, double pixY,
            double pixOrigX, double pixOrigY, double scale) {
        
        // translate relative to pix origin
        pixX = pixX - pixOrigX  ;
        pixY = (pixOrigY - pixY) ; 
        // scale ft to display width and height
        double X = pixX / scale ;
        double Y = pixY / scale ;
        return new MyPoint2D(X, Y) ;
    }

    // we have two pix origins and scales (image and logical)
    // so pass in the ones you want to convert to
    MyPoint2D FtLocToPixLoc(double ftX, double ftY, 
            double pixOrigX, double pixOrigY, double scale) {
        double pixX = scale * ftX + pixOrigX ;
        double pixY = -scale * ftY + pixOrigY ;
        return new MyPoint2D(pixX, pixY) ;
    }

    void toggleZoom() {
        if (mIsZoomed) {
            unZoom() ;
            mIsZoomed = false ;
        }
        else {
            doZoom();
            mIsZoomed = true ;
        }
    }
    
    private void doZoom() {
        if (mSegs.size()>0) {
            calcZoom();

            double cw = mImage.getWidth();
            double ch = mImage.getHeight() ;
            double delX = cw/2.0 - (mZoomWidthPix/2.0 + mZoomPixULx) ;
            double delY = ch/2.0 - (mZoomHeightPix/2.0 + mZoomPixULy) ;
            mPathCanvas.setScaleX(mZoomScale);
            mPathCanvas.setScaleY(mZoomScale);
            mPathCanvas.setTranslateX(delX*mZoomScale);
            mPathCanvas.setTranslateY(delY*mZoomScale);

            mFieldCanvas.setScaleX(mZoomScale);
            mFieldCanvas.setScaleY(mZoomScale);
            mFieldCanvas.setTranslateX(delX*mZoomScale);
            mFieldCanvas.setTranslateY(delY*mZoomScale);

            /***
            System.out.printf("zULx=%g zULy=%g zW=%g zH=%g cW=%g cH=%g scale=%g zoom=%g\n", 
                    mZoomPixULx, mZoomPixULy, mZoomWidthPix, mZoomHeightPix, 
                    mImage.getWidth(), mImage.getHeight(), mImageScale, mZoomScale);
            
            double cw = mImage.getWidth();
            double ch = mImage.getHeight() ;
            double tx = cw/2 ;
            double ty = cw/2 ;
            double zoom = 2 ;
            double dx = cw/2.0 - (mZoomPixULx + mZoomWidthPix/2.0) ;
            double dy = ch/2.0 - (mZoomPixULy + mZoomHeightPix/2.0) ;
            mPathCanvas.setScaleX(zoom);
            mPathCanvas.setScaleY(zoom);
            mPathCanvas.setTranslateX(dx*zoom);
            mPathCanvas.setTranslateY(dy*zoom);

            mFieldCanvas.setScaleX(zoom);
            mFieldCanvas.setScaleY(zoom);
            mFieldCanvas.setTranslateX(dx*zoom);
            mFieldCanvas.setTranslateY(dy*zoom);
            
            System.out.printf("cw=%g zW=%g zULx=%g dx=%g\n", 
                    cw, mZoomWidthPix, mZoomPixULx, dx);
            ***/

        }
    }
    
    private void unZoom() {
        mZoomScale = 1.0 ;
        mAspectRatio = mImage.getWidth()/mImage.getHeight() ;
        mPathCanvas.setTranslateX(0);
        mPathCanvas.setTranslateY(0);            
        mPathCanvas.setScaleX(1.0);
        mPathCanvas.setScaleY(1.0);

        mFieldCanvas.setTranslateX(0);
        mFieldCanvas.setTranslateY(0);
        mFieldCanvas.setScaleX(1.0);
        mFieldCanvas.setScaleY(1.0);
    }

    void toggleBackground() {
        mDrawImage = !mDrawImage ;
        initCanvas(mFieldCanvas);
        draw() ;
    }

    void flipDisplay() {
        mInvertMultiplier = -mInvertMultiplier ;
        initCanvas(mFieldCanvas);
        // draw(null);
        layoutChildren();
    }

    private void initCanvas(Canvas c) {
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, c.getWidth(), c.getHeight());
    }

    private class WindowDims {
        public MyPoint2D ul ;
        public MyPoint2D lr ;
        public double width ;
        public double height ;
    }
}
