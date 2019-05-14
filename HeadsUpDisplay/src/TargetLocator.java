
/*
 * @author paul
 */
public class TargetLocator 
{
    // TODO: add the hatch feed station targets?
    
    // y boundaries for targets
    private static final int NUM_BOUNDARY_LINES = 4 ;
    private static final double[] Y_BOUNDARIES = {14.5, 18.5, 21.0, 22.7} ;
    private static String mLastTarget="none" ;

    // Tables of target orientations vs robot y
    
    // Following are the left rocket targets
    // The array name means "these are the target orientations at various
    // y location thresholds, when the bot orientation is >90 degrees CCW".
    // Note that in my drivetrain methods, a CW yaw angle is converted to
    // CCW and added to a bot starting orientation in CCW.
    // This is because the Dennis Path Generator uses CCW orientations
    // As do all all standard formula for coordinate transformations,
    // regardless of whether an IMU uses pos CW or pos CCW, which varies
    // with manufacturer.
    private static final double[] LEFT_TARGETS_ORIENT_GT_90 = 
                    {-61.6, 0, 61.6, 61.6} ; ;
    // these are the left cargo targets (as we pass the y thresholds)
    private static final double[] LEFT_TARGETS_ORIENT_LT_90 = 
                    {-90, 180, 180, 180} ;
    // these are the right rocket targets (the last one shows up twice)
    private static final double[] RIGHT_TARGETS_ORIENT_LT_90 = 
                    {-118.4, 180, 118.4, 118.4} ; 
    // these are the right cargo ship targets
    private static final double[] RIGHT_TARGETS_ORIENT_GT_90 = 
                    {-90, 0, 0, 0} ;

    // Tables of target locations 
    private static final double[] LEFT_TARGETS_XY_GT_90 = 
               {-11.7,17.3, -10.6,19.1, -11.7,20.9, -11.7, 20.9} ;
    private static final double[] LEFT_TARGETS_XY_LT_90 = 
               {-0.9,18.0, -2.7,21.7, -2.7, 23.5, -2.7, 25.4} ;
    private static final double[] RIGHT_TARGETS_XY_LT_90 = 
               {11.7,17.3, 10.6,19.1, 11.7,20.9, 11.7,20.9} ;
    private static final double[] RIGHT_TARGETS_XY_GT_90 = 
               {0.9,18.0, 2.7,21.7, 2.7, 23.5, 2.7, 25.4} ;

    // target labels
    private static final String[] TARGET_NAME = {"Left Front Rocket", 
        "Left Center Rocket", "Left Back Rocket", "Left Back Rocket", 
        "Left Front Cargo", "Left Side Front Cargo", "Left Side Middle Cargo", 
        "Left Side Back Cargo", "Right Front Rocket", "Right Center Rocket", 
        "Right Back Rocket", "Right Back Rocket", "Right Front Cargo", 
        "Right Side Front Cargo", "Right Side Middle Cargo", 
        "Right Side Back Cargo"} ;
    
    // Figure out target location and orientation based on bot loc and orient 
    public static Position getTargetPosition(Position botPosn) {
        int nameIndex ;
        // Get the target posn and orient from the bot posn and orient
        int index ;
        for (index=0 ; index<NUM_BOUNDARY_LINES ; index++) {
            if (botPosn.y<=Y_BOUNDARIES[index])
                break ;
        }
        double targX, targY, targOrient ;
        if (botPosn.x<0) {
            if (botPosn.orientCCW<90.0) {
                targOrient = LEFT_TARGETS_ORIENT_LT_90[index] ;
                targX = LEFT_TARGETS_XY_LT_90[index*2] ;
                targY = LEFT_TARGETS_XY_LT_90[index*2+1] ;  
                nameIndex = index + 4 ;
            }
            else {
                targOrient = LEFT_TARGETS_ORIENT_GT_90[index] ;
                targX = LEFT_TARGETS_XY_GT_90[index*2] ;
                targY = LEFT_TARGETS_XY_GT_90[index*2+1] ; 
                nameIndex = index ;
            }
        }
        else {           
            if (botPosn.orientCCW<90.0) {
                targOrient = RIGHT_TARGETS_ORIENT_LT_90[index] ;
                targX = RIGHT_TARGETS_XY_LT_90[index*2] ;
                targY = RIGHT_TARGETS_XY_LT_90[index*2+1] ; 
                nameIndex = index + 8 ;
            }
            else {
                targOrient = RIGHT_TARGETS_ORIENT_GT_90[index] ;
                targX = RIGHT_TARGETS_XY_GT_90[index*2] ;
                targY = RIGHT_TARGETS_XY_GT_90[index*2+1] ;                                
                nameIndex = index + 12 ;
            }
        }       
        
        // Subtract the bot position on the field from the target posn
        // in order to make it relative to the bot
        // targX -= botPosn.x ;
        // targY -= botPosn.y ;
        
        Position targPosn = new Position() ;
        targPosn.x = targX ;
        targPosn.y = targY ;
        targPosn.orientCCW = targOrient ;
        
        mLastTarget = TARGET_NAME[nameIndex] ;
        System.out.println("Target is " + TARGET_NAME[nameIndex]);
        
        return targPosn ;
    }
    
    public static String getLastTarget() {
        return mLastTarget ;
    }
}
