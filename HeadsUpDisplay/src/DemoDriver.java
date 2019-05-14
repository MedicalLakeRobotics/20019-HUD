import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javafx.animation.AnimationTimer;

/**
 *
 * @author paul
 */
public class DemoDriver 
{
    private final long TIMER_MSEC = 40 ;
    
    private Main mMain ;
    AnimationTimer mTimer ;
    long mPreviousTime ;
    ArrayList<String> mCmdList = new ArrayList<>() ;
    int mCurrentCmd = 0 ;
    
    public DemoDriver(Main main) {
        mMain = main ;
        
        mTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onTimer(now);
            }
        };
        mTimer.stop();
        
        // open the demo file
        FileInputStream is = null ;
        try {
            is = new FileInputStream("demo.txt") ;
        } catch (FileNotFoundException e) {
            System.out.println("ERROR OPENING DEMO.TXT");
            return ;
        } 
        Scanner scanner = new Scanner(is) ;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine() ;
            mCmdList.add(line) ;
        }        
    }
    
    public void startDemo() {
        if (mCmdList.size()>0) {
            mPreviousTime = System.currentTimeMillis() ;
            mTimer.start();
        }
    }
    
    public void stopDemo() {
        mTimer.stop();
    }
    
    public void onTimer(long now) {
        now = System.currentTimeMillis() ;
        long elapsed = (now-mPreviousTime) ;
        if (elapsed > TIMER_MSEC) { 
            if (mCurrentCmd>=mCmdList.size()) {
                mCurrentCmd = 0 ;
            }
            else {                
                String cmdString = mCmdList.get(mCurrentCmd++) ;
                mMain.serverMessage(cmdString);
            }
            mPreviousTime = now ;
        }
    }
}
