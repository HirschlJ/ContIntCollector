package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Jakobus on 04.11.2015.
 */
public class AccelerometerListener extends AbstractSensorListener
{
    private SensorEventListener accEventListener;

    private String latestResult;
    private int latestAccuracy;

    public AccelerometerListener(SensorManager sensorManager)
    {
        // get sensor and register Event Listener
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accEventListener = new AccSensorListener();
        sensorManager.registerListener(accEventListener, accSensor, 0);
    }

    @Override
    public String measure()
    {
        //Return result and accuracy
        String returnValue = "";
        if(latestResult!=null)
        {
            returnValue += "\"coordinates\" : \"" + latestResult + "\",\n";
            returnValue += "\"accuracy\" : \"" + latestAccuracy + "\"\n";
        }
        else
        {
            returnValue += "\"coordinates\" : null,\n";
            returnValue += "\"accuracy\" : null\n";
        }
        returnValue += "}";
        return returnValue;
    }

    @Override
    public String getName()
    {
        return "\"acc\" : \n{\n";
    }

    @Override
    public void stopListener(SensorManager sensorManager)
    {
        sensorManager.unregisterListener(accEventListener);
    }

    private class AccSensorListener implements SensorEventListener
    {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            //get result and put them in one string
            if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                latestResult = String.valueOf(event.values[0]) + String.valueOf(event.values[1]) + " " +String.valueOf(event.values[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
            latestAccuracy = accuracy;
        }
    }
}
