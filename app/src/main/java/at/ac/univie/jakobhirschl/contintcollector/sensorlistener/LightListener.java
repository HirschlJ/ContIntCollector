package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Jakobus on 03.11.2015.
 */
public class LightListener extends AbstractSensorListener
{
    private SensorEventListener lightSensorListener;
    private String latestValue;
    private int latestAccuracy;

    public LightListener(SensorManager sensorManager)
    {
        //get sensor and register listener
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightSensorListener = new LightSensorListener();
        sensorManager.registerListener(lightSensorListener, lightSensor, 0);
    }

    @Override
    public String measure()
    {
        //Return result and accuracy
        String returnValue = "";
        if(latestValue!=null)
        {
            returnValue += "\"lux\" : \"" + latestValue + "\",\n";
            returnValue += "\"accuracy\" : \"" + latestAccuracy + "\"\n";
        }
        else
        {
            returnValue += "\"lux\" : null,\n";
            returnValue += "\"accuracy\" : null\n";
        }
        returnValue += "}";
        return returnValue;
    }

    @Override
    public String getName()
    {
        return "\"light\" : \n{\n";
    }

    @Override
    public void stopListener(SensorManager sensorManager)
    {
        sensorManager.unregisterListener(lightSensorListener);
    }

    private class LightSensorListener implements SensorEventListener
    {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            //change lastest value if event is light event
            if( event.sensor.getType() == Sensor.TYPE_LIGHT) latestValue = String.valueOf(event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
            latestAccuracy = accuracy;
        }
    }
}
