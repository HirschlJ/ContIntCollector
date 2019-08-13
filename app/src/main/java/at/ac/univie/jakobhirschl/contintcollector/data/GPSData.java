package at.ac.univie.jakobhirschl.contintcollector.data;

/**
 * Created by Jakob Hirschl on 19.11.2015.
 *
 * Class holding GPS Data
 *
 */
public class GPSData
{
    private double latitude;
    private double longitude;
    private double speed;
    private double accuracy;

    public GPSData(double latitude, double longitude, double speed, double accuracy)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.accuracy = accuracy;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public double getSpeed()
    {
        return speed;
    }

    public void setSpeed(double speed)
    {
        this.speed = speed;
    }

    public double getAccuracy()
    {
        return accuracy;
    }
}
