package at.ac.univie.jakobhirschl.contintcollector.data;

import android.text.InputType;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;

/**
 * Created by Jakobus on 13.11.2015.
 *
 * Class holding and preparing all data fro visualization
 *
 */
public class SessionData
{
    private int ID;
    //Information about Data
    private boolean useAccelerometer=true;
    private boolean useGPS=true;
    private boolean useLight=true;
    private boolean useNoise=true;
    private boolean useInteraction=true;
    private boolean useBluetooth=true;

    //Data
    private List<Long> timestamp = new ArrayList<>();
    private List<GPSData> gpsData = new ArrayList<>();
    private List<String> accData;
    private List<Double> lightData = new ArrayList<>();
    private List<Double> lightAccuracy = new ArrayList<>();
    private List<Double> noiseData = new ArrayList<>();
    private List<Integer> intData = new ArrayList<>();
    private List<Integer> bluetoothData = new ArrayList<>();

    public SessionData(int ID, List<JSONObject> jsonObjects, boolean smooth) throws Exception
    {
        this.ID = ID;
        //check if there is any data
        if(!(jsonObjects.get(0).has("gps"))) useGPS=false;
        if(!(jsonObjects.get(0).has("acc"))) useAccelerometer=false;
        if(!(jsonObjects.get(0).has("light"))) useLight=false;
        if(!(jsonObjects.get(0).has("noise"))) useNoise=false;
        if(!(jsonObjects.get(0).has("interaction"))) useInteraction=false;
        if(!(jsonObjects.get(0).has("bluetooth"))) useBluetooth=false;
        //for each measurement
        for(int i = 0; i < jsonObjects.size(); i++)
        {
            if(useGPS)
            {
                double longitude, latitude, speed, accuracy;
                JSONObject gpsJSON = jsonObjects.get(i).getJSONObject("gps");
                //Convert GPS Data
                //assign long and lat
                if(!gpsJSON.isNull("latitude"))latitude = Double.parseDouble(gpsJSON.get("latitude").toString());
                else latitude = -1;
                if(!gpsJSON.isNull("longitude"))longitude = Double.parseDouble(gpsJSON.get("longitude").toString());
                else longitude = -1;
                //if there is no speed -> assign -1 (for missing value)
                if(!gpsJSON.isNull("speed"))speed = Double.parseDouble(gpsJSON.get("speed").toString());
                else speed = -1;
                //100 -> penalty for missing data, is not used anyway
                if(!gpsJSON.isNull("accuracy"))accuracy = Double.parseDouble(gpsJSON.get("accuracy").toString());
                else accuracy = -1;
                Log.i("Test", (new GPSData(latitude,longitude,speed,accuracy)).toString());
                this.gpsData.add(new GPSData(latitude,longitude,speed,accuracy));
            }
            if(useLight)
            {
                JSONObject lightJSON = jsonObjects.get(i).getJSONObject("light");
                this.lightData.add(Double.parseDouble(lightJSON.get("lux").toString()));
                lightAccuracy.add(Double.parseDouble(lightJSON.get("accuracy").toString()));
            }
            if(useNoise) this.noiseData.add(Double.parseDouble(jsonObjects.get(i).getJSONObject("noise").get("dB").toString()));
            if(useBluetooth) this.bluetoothData.add(Integer.parseInt(jsonObjects.get(i).getJSONObject("bluetooth").get("numOfBTDevices").toString()));
            if(useInteraction) this.intData.add(Integer.parseInt(jsonObjects.get(i).getJSONObject("interaction").get("touch").toString()));
            this.timestamp.add(Long.parseLong(jsonObjects.get(i).get("timestamp").toString()));
        }
        //If speed was not measured for the first or last measurement -> set it 0 so the line can be drawn later on
        if(useGPS)
        {
            if(this.gpsData.get(0).getSpeed()==-1) this.gpsData.get(0).setSpeed(0.0);
            if(this.gpsData.get(this.gpsData.size()-1).getSpeed()==-1) this.gpsData.get(this.gpsData.size()-1).setSpeed(0.0);
        }

        if(smooth)
        {
            if(useGPS) smoothGPSData();
            if(useNoise) smoothNoiseData();
            if(useLight) smoothLightData();
        }
        this.ID = ID;

    }

    private void smoothGPSData()
    {
        //5-day moving average
        for(int i = 2; i < (gpsData.size()-2); i++)
        {
            if(gpsData.get(i).getSpeed()!=-1.0&&gpsData.get(i+1).getSpeed()!=-1.0&&gpsData.get(i+2).getSpeed()!=-1.0&&gpsData.get(i-1).getSpeed()!=-1.0&&gpsData.get(i-2).getSpeed()!=-1.0)
            {
                double avgSpeed = (gpsData.get(i).getSpeed()+gpsData.get(i+1).getSpeed()+gpsData.get(i+2).getSpeed()+gpsData.get(i-1).getSpeed()+gpsData.get(i-2).getSpeed())/5;
                gpsData.get(i).setSpeed(avgSpeed);
            }
        }
    }
    private void smoothLightData()
    {
        //5-day moving average
        for(int i = 2; i < (lightData.size()-2); i++)
        {
            double avgLight = (lightData.get(i)+lightData.get(i + 1)+lightData.get(i + 2)+lightData.get(i - 1)+lightData.get(i - 2))/5;
            lightData.set(i, avgLight);
        }
    }
    private void smoothNoiseData()
    {
        //5-day moving average
        for(int i = 2; i < (noiseData.size()-2); i++)
        {
            double avgNoise = (noiseData.get(i)+noiseData.get(i+1)+noiseData.get(i+2)+noiseData.get(i-1)+noiseData.get(i-2))/5;
            noiseData.set(i, avgNoise);
        }
    }

    public List<Double> getTimeSpan()
    {
        List<Double> list = new ArrayList<>();
        for(long value : timestamp)
        {
            list.add(((double)(value-timestamp.get(0)))/1000.0);
        }
        return list;
    }

    public int getID()
    {
        return ID;
    }

    public List<Long> getTimestamp()
    {
        return timestamp;
    }

    public List<String> getAccData()
    {
        return accData;
    }

    public boolean isUseAccelerometer()
    {
        return useAccelerometer;
    }

    public boolean isUseGPS()
    {
        return useGPS;
    }

    public boolean isUseLight()
    {
        return useLight;
    }

    public boolean isUseNoise()
    {
        return useNoise;
    }

    public boolean isUseInteraction()
    {
        return useInteraction;
    }

    public boolean isUseBluetooth()
    {
        return useBluetooth;
    }

    public List<GPSData> getGpsData()
    {
        return gpsData;
    }

    public List<Double> getLightData()
    {
        return lightData;
    }

    public List<Double> getLightAccuracy()
    {
        return lightAccuracy;
    }

    public List<Double> getNoiseData()
    {
        return noiseData;
    }

    public List<Integer> getIntData()
    {
        return intData;
    }

    public List<Integer> getBluetoothData()
    {
        return bluetoothData;
    }
}
