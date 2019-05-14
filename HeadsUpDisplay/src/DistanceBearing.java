
/**
 * @author Paul
 */
public class DistanceBearing {
    public double distance ;
    public double bearingCW ;
    
    public DistanceBearing() {}
    
    public DistanceBearing(DistanceBearing db) {
        distance = db.distance ;
        bearingCW = db.bearingCW ;
    }

    public DistanceBearing(double distance, double bearingCW) {
        this.distance = distance ;
        this.bearingCW = bearingCW ;
    }
}
