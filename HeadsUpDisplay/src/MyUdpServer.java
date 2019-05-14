
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class MyUdpServer 
{
    public interface UdpServerCallback {
        public void serverMessage(String udpServerMessage) ;
    }
    
    private UdpServerCallback mCallback = null ;
    private UdpServer mServer = null ;
    
    // pass a null if you want to poll instead of getting a callback
    public MyUdpServer(UdpServerCallback callback) {
        mCallback = callback ;
    }

    // start the server thread on a given port
    public void startUdpServer(int port) {
        if (mCallback!=null)                    
            mCallback.serverMessage("Starting Udp server on port " + port) ;
        try {
            mServer = new UdpServer(port);
            mServer.start();
        } catch(IOException e) {
            if (mCallback!=null)
                mCallback.serverMessage("Exception starting server " + e.getMessage()) ;
        }
    }
    
    public void stopUdpServer() {
    	if (mServer != null) {
    	    mServer.interrupt();
            if (mCallback!=null)                    
                mCallback.serverMessage("Stopping UDP Server");    		
    	}
    	mServer = null ;        // allow garbage collection
    }
    
    public boolean isRunning() {
    	return (mServer != null) ;
    }
    
    public String getLastMessage() {
    	if (mServer != null)
    	    return mServer.getDgramString() ;
    	else
    	    return "(Not Running)" ;
    }

    // Server class implemented as an inner class
    class UdpServer extends Thread 
    { 
        private DatagramSocket mSocket;
        // buffer for receiving socket data
        private byte[] mBuf = new byte[256] ;
        // string version of what was received
        private String mDgram = "(No Msg)" ;
        private String mPolledDgram = "(No Msg)" ;

        // PHS NOTE: port>=1024 to avoid java.net.BindException: Permission denied
        public UdpServer(int port) throws IOException {
            // open a datagram socket on the indicated port
            // we accept connections from any client on that port
            mSocket = new DatagramSocket(port);
            // allow receive to block for only this amount of msec
            // this allows the trhead to periodically check for interrupted()
            // timeout will fire SocketTimeoutException below, but the socket will remain valid
            mSocket.setSoTimeout(1000);
        }

        // another thread can read the datagram by calling this
        // synchronization is not necessary because this thread only fills mBuf with 
        // DatagramSocket.receive() which is atomic, as is the assignment to mDgram below,
        // as is the copy of the returned reference
        private String getDgramString() {
            return mPolledDgram ;
        } 

        // we extended thread, so all communication occurs in this run method    
        public void run() {
            long msgNum = 0 ;
            while (!isInterrupted()) {
                // create a packet for up to our buffer length
                // if the message is longer than this, it will be truncated
                // if shorter, the packet length will shrink 
                // (thus we create a new one each time)
                DatagramPacket packet = new DatagramPacket(mBuf, mBuf.length);
                try {
                    // Receive the packet. This will block or timeout and should be atomic.
                    mSocket.receive(packet);
                    msgNum++ ;
                    mDgram = new String(packet.getData(), 0, packet.getLength());
                    mPolledDgram = String.format("%d: %s", msgNum, mDgram) ;
                    if (mCallback!=null)
                        mCallback.serverMessage(mDgram) ;

                    // we allow a client to shut us down as well
                    if (mDgram.equals("*** STOP ***")) {
                        // yes, we can interrupt ourself, which will exit this loop
                        interrupt();   
                        if (mCallback!=null)                    
                            mCallback.serverMessage("Udp server quitting") ;
                    }
                } 
                catch (SocketTimeoutException to) {
                    // for a timeout, just make a note and try again
                    // mCallback.serverMessage("Udp server receive timed out") ;
                }
                catch (IOException e) {
                    // for any other exception, maybe we should quit?
                    if (mCallback!=null)                    
                        mCallback.serverMessage("Server exception: " + e.getMessage()) ;
                }
            }
            mSocket.close();
        }    
    }

}

