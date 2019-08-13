package at.ac.univie.jakobhirschl.contintcollector.appdata;

import android.content.Context;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Jakob Hirschl on 14.11.2015.
 *
 * Settings Data is saved here.
 *
 */
public class AppData implements Serializable
{
    public boolean serviceRunning = false;

    public boolean useAccelerometer = false;
    public boolean useGPS = true;
    public boolean useLight = true;
    public boolean useNoise = true;
    public boolean useInteraction = true;
    public boolean useBluetooth = true;

    public long interval = 1000;

    public int interpolation = 10;

    public boolean smooth = true;

    public double referenceAmpl = 1;

    public double minLonLat = 0.000001;

    public void serialize(Context context)
    {
        try
        {
            FileOutputStream fos = context.openFileOutput("appData.ser", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
            Log.i("ApPData","Data Serialized");
        }
        catch (Exception e)
        {
            Log.i("AppData","Data Serialization failed!");
        }
    }

    public void readData(Context context) throws Exception
    {
        FileInputStream fis = context.openFileInput("appData.ser");
        ObjectInputStream is = new ObjectInputStream(fis);
        AppData readObject = (AppData) is.readObject();
        is.close();
        fis.close();
        copyData(readObject);
        Log.i("ApPData", "Data read");
    }

    private void copyData(AppData readObject)
    {
        this.serviceRunning = readObject.serviceRunning;
        this.useAccelerometer = readObject.useAccelerometer;
        this.useGPS = readObject.useGPS;
        this.useLight = readObject.useLight;
        this.useNoise = readObject.useNoise;
        this.useInteraction = readObject.useInteraction;
        this.useBluetooth = readObject.useBluetooth;
        this.referenceAmpl = readObject.referenceAmpl;
        this.smooth = readObject.smooth;
        this.interval = readObject.interval;
        this.interpolation = readObject.interpolation;
        this.minLonLat = readObject.minLonLat;
    }

    public boolean[] getBooleanArray()
    {
        return new boolean[]{useGPS,useInteraction,useNoise,useLight,useAccelerometer,useBluetooth};
    }
}
