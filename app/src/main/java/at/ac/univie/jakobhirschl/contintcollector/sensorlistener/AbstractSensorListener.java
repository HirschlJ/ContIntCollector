package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;

import android.hardware.SensorManager;

/**
 * Created by Jakobus on 03.11.2015.
 */
public abstract class AbstractSensorListener
{
    //returns all JSON values e.g. "lux" : "86", "accuracy" : "0.1" }
    public abstract String measure();

    //returns the JSON name e.g. "lightData":{
    public abstract String getName();

    public abstract void stopListener(SensorManager sensorManager);
}
