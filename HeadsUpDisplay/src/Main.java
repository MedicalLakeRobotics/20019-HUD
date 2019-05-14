import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.opencv.core.Core;

/**
 * @author paul
 */
public class Main extends Application implements MyUdpServer.UdpServerCallback
{
    private enum ScreenType {OVERHEAD, VIDEO}
    ScreenType mCurrentScreen = ScreenType.OVERHEAD ;
    
    private Label      mStatus = new Label("Welcome to Heads Up Display") ;
    public  BorderPane mRoot = new BorderPane();
    public  Stage      mStage ;
    public  Scene      mScene ;

    private DemoDriver  mDemoDriver ;
    private DennisPathGen  mPathGenerator ;
    private HeadsUpDisplay mHudDisplay ;  
    private MyUdpServer    myUdpServer ;
    private int mUdpMsgPortNum;
    
    private Image mLightImage, mDarkImage ;
    private boolean mIsLightTheme = true ;
    private boolean  mFlipDisplay = false ;
    private boolean mDemoIsRunning = false ;
    
    private boolean  mHaveTargetPosn = false ;
    private Position mTargPosn ;
    private Position mTargPosnNorm = new Position() ;
    private String mTargetName ;
    private Position mBotPosn ;
    private Position mBotPosnNorm = new Position() ;
    private DistanceBearing mDBtoTarg = new DistanceBearing();

    private IpVideo mIpVideo ;
    private String mVideoIpAddr ;
    private String mVideoIpDir;
    private Integer mVideoIpPort ;
    private ImageView mVideoImageView ;
    private int mVideoWidth, mVideoHeight ;

    // this is where the program starts
    @Override
    public void start(Stage primaryStage) {    
        //System.out.println("entered start") ;
        mStage = primaryStage ;
        mRoot.setStyle("-fx-background-color: lightgray;") ;
        primaryStage.setOnCloseRequest(event -> exit()) ;

        //System.out.println("creating HUD") ;
        // create the HUD screen and put it in the center
        mHudDisplay = new HeadsUpDisplay(this) ;
        
        //System.out.println("reading settings") ;
        // read the settings file
        if (!readIniSettings()) {
            System.out.println("ERROR READING INI FILE");
            exit();
        }

        //System.out.println("starting UDP server") ;
        // get a UDP server running
        myUdpServer = new MyUdpServer(this) ;          
        // FRC has 5800-5810 open
        myUdpServer.startUdpServer(mUdpMsgPortNum);
        
        //System.out.println("creating video streamer") ;
        // create a vision display
        mVideoImageView = new ImageView() ;
        mIpVideo = new IpVideo(this, mVideoImageView, mFlipDisplay, 
                mVideoIpAddr, mVideoIpDir, mVideoIpPort, mVideoWidth, mVideoHeight) ;
        
        if (mCurrentScreen == ScreenType.OVERHEAD)
            mRoot.setCenter(mHudDisplay);
        else
            gotoVideo() ;

        // set up the window
        mRoot.setBottom(new ToolBar(mStatus));
        mScene = new Scene(mRoot, mLightImage.getWidth(), mLightImage.getHeight());
        mScene.setOnKeyPressed(keyEvent -> onKeyPressed(keyEvent));
        primaryStage.setTitle("Heads Up Display");
        primaryStage.setScene(mScene);
        primaryStage.show();
        
        // store the path generator in a member var for convenience
        mPathGenerator = DennisPathGen.getInstance() ;
        
        //System.out.println("creating a demo driver") ;
        // create a demo object (could wait until the key D is pressed)
        mDemoDriver = new DemoDriver(this) ;
    }
    
    @Override
    public void serverMessage(String udpMessage) {
        // This allows us to call this from another thread
        // TODO: putting the UDP server in an AsyncTask would be better
        // Or we could poll from a timer, but that's not very efficient
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                setStatus(udpMessage);
                processUdpMessage(udpMessage) ;
            }
        } ) ;
    }

    private void processUdpMessage(String str) {

        str = str.toLowerCase() ;
        if (str.contains("*** exit ***")) exit() ;
        
        // parse the message into fields
        String[] fields = str.split(",") ;
        
        String msg1 = null ;
        String msg2 = null ;
        switch (fields[0]) {
            case "calculate":
                // parse bot position and move it on the display
                mBotPosn = parseLoc(fields,1) ;
                // The orientation in this message is the current orientation
                // of the robot, relative to the field y axis, CW. The path 
                // generator code needs to do coordinate system transformations, 
                // and needs to know the orientation of the robot CCW.
                mBotPosn.orientCCW = 90.0 - mBotPosn.orientCCW ;
                // if haven't been given a target yet, revert to internal
                // calculation for the target location, which is determined
                // from the context of the bot position
                mHudDisplay.setPositions(mBotPosn) ;
                if (!mHaveTargetPosn) {
                    mTargPosn = TargetLocator.getTargetPosition(mBotPosn) ;
                    mTargetName = TargetLocator.getLastTarget() ;
                }
                msg1 = calcPath() ;
                if (msg1 != null)
                    msg1 += " to " + mTargetName ;
                mHudDisplay.setMessages(msg1, msg2) ;
                break ;
            case "position":
                // parse bot position and move it on the display
                mBotPosn = parseLoc(fields,1) ;
                mBotPosn.orientCCW = 90.0 - mBotPosn.orientCCW ;
                mHudDisplay.setPositions(mBotPosn) ;
                break ;
            case "target":
                mTargetName = fields[1] ;
                mTargPosn = parseLoc(fields,2) ;
                // The orientation in this message is the final orientation
                // of the robot, relative to the field y axis, CW. The path 
                // generator code needs to do coordinate system transformations, 
                // so it needs to know the orientation of the target's axis 
                // system, relative to the the field axis system, CCW.
                mTargPosn = convertBotOrientToTargetOrient(mTargPosn) ;
                mHaveTargetPosn = true ;
                break ;
            case "time":
                msg1 = fields[1] + fields[2] ;
                mHudDisplay.setMessages(msg1, msg2) ;
                break ;
            case "robotstate":
                msg2 = fields[1] ;
                mHudDisplay.setMessages(msg1, msg2) ;
                break ;
            case "window":
                toggleWindow() ;
                break ;
            case "clearmsgs":
                mHudDisplay.setMessages(null, null) ;
                mHudDisplay.draw();
                break ;
            case "vision":
                mDBtoTarg.distance = Double.parseDouble(fields[1]);
                mDBtoTarg.bearingCW = Double.parseDouble(fields[2]);
                break ;
            case "zoom":
                mHudDisplay.toggleZoom();
                break ;
            case "iconify":
                mStage.setIconified(true);
                break ;
            case "maximize":
                mStage.setMaximized(true);
                break ;
            case "theme":
                switchTheme();
                break ;
            default:
                return ;
        }
        
        // System.out.printf("Drawing Bot at (%g,%g)\n", botPosn.x, botPosn.y);
        // System.out.printf("Target at (%g,%g)\n", mTargPosn.x, mTargPosn.y); 
        mHudDisplay.draw() ;
    }
    
    Position parseLoc(String[] fields, int start) {
        Position posn = new Position() ;
        posn.x = Double.parseDouble(fields[start]) ;
        posn.y = Double.parseDouble(fields[start+1]) ;
        posn.orientCCW = Double.parseDouble(fields[start+2]) ;
        //System.out.println(fields[1]+" "+fields[2]);
        //System.out.printf("x=%g y=%g\n", posn.x, posn.y);
        return posn ;
    }

    private String calcPath() {
        // this is a member var because we'll need to pass it
        // to the drawing routine
        //mFlipDisplay = false ;
        
        // first convert positions to be relative to bot start
        // in this case the current bot posn is the bot start
        mTargPosnNorm.x = mTargPosn.x - mBotPosn.x ;
        mTargPosnNorm.y = mTargPosn.y - mBotPosn.y ;
        mTargPosnNorm.orientCCW = mTargPosn.orientCCW ;
        mBotPosnNorm.x = 0 ;
        mBotPosnNorm.y = 0 ;
        mBotPosnNorm.orientCCW = mBotPosn.orientCCW ;
        
        // now normalize those to the target frame of reference
        convertBotToNorm();
        
        // initialize the path generator with a target standoff of 0
        mPathGenerator.init(0);
        
        ArrayList<Segment> fieldSegs = new ArrayList<>() ;
        try {
            // update the path using bot normalized position
            mPathGenerator.pathUpdate(mBotPosnNorm) ;
        } catch (NoPathException ex) {
            // System.out.println("*** NO PATH FOUND ***");
            mHudDisplay.setSegments(fieldSegs);
            return "No Path" ;
        }
        
        // now convert segments from the target to the field frame of reference
        ArrayList<Segment> segs = DennisPathGen.getInstance().mSegments ;
        for (int i=0 ; i<segs.size() ; i++) {
            Segment seg = segs.get(i) ;
            Segment segField = seg.convertToFieldCoords(
                    mTargPosn.x, mTargPosn.y, mTargPosn.orientCCW) ;
            fieldSegs.add(segField) ;
        }
        mHudDisplay.setSegments(fieldSegs);
        
        return null ;
    }
    
    private void convertBotToNorm() {        
        // Here we express bot locations in a rotated coordinate system
        // relative to the target. Target rotation is defined as 
        // rotation of the entry CCW from horizontal.
        double theta = mTargPosnNorm.orientCCW * Math.PI / 180.0 ;
        
        // first we translate to the target frame
        double botRelTransX = mBotPosnNorm.x - mTargPosnNorm.x ;
        double botRelTransY = mBotPosnNorm.y - mTargPosnNorm.y ; ;
        
        // now rotate into the target frame
        double cos = Math.cos(theta) ;
        double sin = Math.sin(theta) ;
        mBotPosnNorm.x = cos*botRelTransX + sin*botRelTransY ;
        mBotPosnNorm.y = -sin*botRelTransX + cos*botRelTransY ;     
        
        // now the bot orientation relative to the target frame 
        mBotPosnNorm.orientCCW = mBotPosn.orientCCW - theta*180.0/Math.PI ;        
        if (mBotPosnNorm.orientCCW<0) mBotPosnNorm.orientCCW += 360 ;
        if (mBotPosnNorm.orientCCW>360) mBotPosnNorm.orientCCW -= 360 ;
    }
       
    public void setStatus(String status) {
        mStatus.setText(status);
    }

    // TODO: read the image name, image scale, and origin location and
    // whether to actually draw it from the settings.ini file
    boolean readIniSettings() {
        FileInputStream is = null ;
        try {
            is = new FileInputStream("settings.ini") ;
        } catch (FileNotFoundException e) {
            // TODO: change this to a popup
            exit() ;
        }
        //System.out.printf("Found ini file OK\n") ;
        
        // settings to read from the file
        boolean bkgrnd = true ;
        boolean autoZoom = false ;
        double ppi = 1 ;
        double origX = 10 ;
        double origY = 10 ;
        boolean mirrorDisplay = false ;

        Scanner scanner = new Scanner(is) ;
        int lineNum = 0 ;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine() ;
            if (line.isEmpty() || line.charAt(0)=='%')
                continue ;
            line = line.toLowerCase() ;
            switch (lineNum) {
                case 0: bkgrnd = parseBoolean(line) ; 
                        break ;
                case 1: mLightImage = openImage(line) ;
                        if (mLightImage==null) exit() ;
                        break ;
                case 2: mDarkImage = openImage(line) ;
		                if (mDarkImage==null) exit() ;
		                break ;
                case 3: if (line.compareTo("light")==0)
                            mIsLightTheme = true ;
                        else
                            mIsLightTheme = false ;
                        break ;
                case 4: ppi = Double.parseDouble(line) ; 
                        break ;
                case 5: origX = Double.parseDouble(line) ; 
                        break ;
                case 6: origY = Double.parseDouble(line) ; 
                        break ;
                case 7: mUdpMsgPortNum = Integer.parseInt(line) ;
                        break ;
                case 8: autoZoom = parseBoolean(line) ;
                        break ;
                case 9: mirrorDisplay = parseBoolean(line) ;
                        break ;
                case 10: mVideoIpAddr = line ;
                         break ;
                case 11: mVideoIpDir = line ;
                         break ; 
                case 12: mVideoIpPort = Integer.parseInt(line) ;
                         break ;
                case 13: mVideoWidth = Integer.parseInt(line) ;
                         break ;
                case 14: mVideoHeight = Integer.parseInt(line) ;
                         break ;
                case 15: if (line.compareTo("hud")==0)
                            mCurrentScreen = ScreenType.OVERHEAD ;
                         else
                            mCurrentScreen = ScreenType.VIDEO ;
                         break ;
                default:
                    break ;
            }
            lineNum++ ;
        }
        scanner.close();
        if (lineNum==16) {
            double mirrorFactor = mirrorDisplay ? -1.0 : 1.0 ;
            Image img = mIsLightTheme ? mLightImage : mDarkImage ;        
            mHudDisplay.setSettings(bkgrnd, img, ppi, origX, origY, 
                    autoZoom, mirrorFactor) ;
            return true ;            
        }
        return false ;       
    }
    
    public boolean parseBoolean(String s) {
        s = s.toLowerCase() ;
        if (s.contains("yes")) return true ;
        if (s.contains("true")) return true ;
        return false ;
    }
    
    public Image openImage(String fname) {
        File file = new File(fname) ;
        Image img ;
        try {
            BufferedImage bImage = ImageIO.read(file);
            img = SwingFXUtils.toFXImage(bImage, null);
        } catch (Exception ex) {
            // TODO: pop up a dialog box here
            System.out.printf("*** ERROR LOADING IMAGE FILE %s ***\n", fname);
            return null;
        }
        return img ;
    }    

    // The incoming "target orientation" is actually the desired 
    // final orient of the robot, relative to the field y axis, CW. The path 
    // generator code needs to do coordinate system transformations, 
    // so it needs to know the orientation of the target's axis 
    // system, relative to the the field axis system, CCW.
    // This routine does the conversion.
    private Position convertBotOrientToTargetOrient(Position targPosn) {
        double botOrient = targPosn.orientCCW ;
        targPosn.orientCCW = -(botOrient+90) ;
        return targPosn ;
    }

    private void onKeyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case Z:
                mHudDisplay.toggleZoom() ;
                break;
            case B:
                mHudDisplay.toggleBackground() ;
                break ;
            case F:
                mHudDisplay.flipDisplay() ;
                mFlipDisplay = ! mFlipDisplay ;
                mIpVideo.setFlip(mFlipDisplay);
                break ;
            case W:
                toggleWindow() ;
                break ;
            case M:
                if (!mStage.isMaximized())
                    mStage.setMaximized(true);
                else
                    mStage.setMaximized(false);
                break ;
            case T:
                switchTheme();
                break ;
            case D:
                if (mDemoIsRunning)
                    mDemoDriver.stopDemo() ;
                else 
                    mDemoDriver.startDemo() ;
                mDemoIsRunning = !mDemoIsRunning ;
            default:
            	break ;
        }
    }

    private void toggleWindow() {
        setStatus("switched window");
        if (mCurrentScreen==ScreenType.OVERHEAD) {
            boolean success = gotoVideo() ;
            if (!success)
                setStatus("connection to camera FAILED");
        } else {
            mCurrentScreen =ScreenType.OVERHEAD ;
            mIpVideo.stop();
            mRoot.setCenter(mHudDisplay);
        }
    }
    
    private boolean gotoVideo() {
        boolean success = mIpVideo.start();
        if (success) {
            mCurrentScreen =ScreenType.VIDEO ;
            mRoot.setCenter(mVideoImageView);
        }
        else {
            mCurrentScreen =ScreenType.OVERHEAD ;
            mIpVideo.stop();
            mRoot.setCenter(mHudDisplay);            
        }
        return success ;
    }
    
    private void switchTheme() {
        if (mIsLightTheme) {
            mIsLightTheme = false ;
            mHudDisplay.setImage(mDarkImage);
        }
        else {
            mIsLightTheme = true ;
            mHudDisplay.setImage(mLightImage);
        }
    }
   
    private void exit() {
        //mTimer.stop() ;
        if (myUdpServer != null)
        	myUdpServer.stopUdpServer();  
        if (mIpVideo != null)
            mIpVideo.stop() ;
        Platform.exit();
    }

    public static void main(String[] args) {
        System.out.println("Welcome to OpenCV " + Core.VERSION 
                            + " " + Core.NATIVE_LIBRARY_NAME);
        
        // suppress system errors that could slow down frame rate
        System.err.close();
        
        // load the native OpenCV library
        //System.out.println("opening: "+Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);   
        //System.load("c:\\opencv_java401.dll");
        //System.load("c:\\opencv_ffmpeg401_64.dll");
        
        //System.out.println("launching");
        launch(args);
    }

}
