/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.sound.sampled.Port;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 *
 * @author paul
 */
public class IpVideo 
{
    private final long TIMER_MSEC = 0 ;
    
    private Main mMain ;
    AnimationTimer mTimer ;
    long mPreviousTime ;
    private VideoCapture mCapture ;
    boolean mFlip = false ;
    private ImageView mImageView ;   
    private String mIpAddr ;
    private String mIpDir ;
    private int mIpPort ;
    private boolean mDoResize ;
    int mVideoWidth, mVideoHeight ;
    
    public IpVideo(Main main, ImageView imageView, boolean flip, String ipAddr, 
            String dir, int vidPort, int vidWidth, int vidHeight) 
    {
        mMain = main ;
        mCapture = new VideoCapture();
        mImageView = imageView ;  
        mIpAddr = ipAddr ;
        mIpDir = dir ;
        mIpPort = vidPort ;
        mFlip = flip ;
        mVideoWidth = vidWidth ;
        mVideoHeight = vidHeight ;
        if (vidHeight==0 || vidWidth==0)
            mDoResize = false ;
        else
            mDoResize = true ;
        mTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onTimer(now);
            }
        };
        mTimer.stop();
    }
    
    public void setFlip(boolean flip) {
        mFlip = flip ;
    }
    
    public boolean start() {
       
        if (testPing()) {
            mCapture.open("http://"+mIpAddr+":"+mIpPort+mIpDir) ;

            // is the video stream available?
            if (mCapture.isOpened()) {
                mPreviousTime = System.currentTimeMillis() ;
                mTimer.start();
                return true ;
            }
        }
        return false ;
    }
    
    public void stop() {
        mTimer.stop();
        if (mCapture.isOpened()) {
            // release the camera
            mCapture.release();
        }           
    }
    
    private void onTimer(long now) {
        now = System.currentTimeMillis() ;
        long elapsed = (now-mPreviousTime) ;
        if (elapsed > TIMER_MSEC) {
            // grab and process a single frame
            Mat frame = grabMatFrame();
            // convert and show the frame
            Image imageToShow = Utils.mat2Image(frame);
            mImageView.setImage(imageToShow);
            mPreviousTime = now ;
        }
    }
       
    private Mat grabMatFrame() {
        Mat frame = new Mat();
        // check if the capture is open
        if (mCapture.isOpened()) {
            try {
                // read the current frame
                // mCapture.read(frame);
                boolean gotit = mCapture.grab();
                // if the frame is not empty, process it
                // if (!frame.empty()) {
                if (gotit) {
                    if (mCapture.retrieve(frame)) { 
                        // Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                        // NOTE: some cameras will mirror image, probably faster
                        if (mFlip) Core.flip(frame, frame, 0);
                        if (mDoResize) {
                           Mat temp = frame ;
                           Imgproc.resize(temp, frame, new Size(1280, 720), Imgproc.INTER_NEAREST) ;                            
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception during frame read: " + e);
            }
        }
        return frame;
    }  
    
    private boolean testPing() {
        boolean success = false ;
        
        Runtime rt = Runtime.getRuntime();
        String command = "ping -n 1 -i 1 -w 1000 "+mIpAddr ;
        // System.out.println(command);
        Process proc;
        try {
            proc = rt.exec(command);
        } catch (IOException ex) {
            System.out.println("error performing system exec");
            return false ;
        }
        BufferedReader stdInput = 
              new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // read the output from the command
        // System.out.println("Here is the standard output of the command:\n");
        String s = null;
        int line=1 ;
        try {
            while ((s = stdInput.readLine()) != null) {
                System.out.println(line++ + ": " + s);
                if (s.contains("bytes=")) {
                    success = true ;
                    break ;
                }
                if (s.contains("timed out"))
                    break ;
                if (s.contains("TTL expired"))
                    break ;
            }
        } catch (IOException ex) {
            System.out.println("io error reading stdinput");            
        }
        // System.out.println("the result is: " + success);
        return success;
    }
    
    /***
    private boolean checkViaFakePing() {
        String hello = "hello" ;
        byte[] buf = hello.getBytes() ;
        InetAddress server ;
        
        DatagramSocket socket ;
        try {
            server = InetAddress.getByName(mIpAddr) ;
            socket = new DatagramSocket() ;
        } catch (SocketException ex) {
            return false ;
        } catch (UnknownHostException ex) {
            return false ;
        }

        // no point - the above will fail if can't open a socket there
        // try the echo port (7)
        // maybe see Just Java 2, Peter van der Linden
        //        DatagramPacket packet = new DatagramPacket(buf, buf.length, server, 7);
        //        try {
        //            socket.send(packet);
        //        }
        //        catch (IOException e) {
        //            return false ;
        //        }
        return true ;
    }
    ***/
}
