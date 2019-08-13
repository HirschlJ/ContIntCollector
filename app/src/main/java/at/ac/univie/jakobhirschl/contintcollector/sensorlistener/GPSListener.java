package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Jakobus on 04.11.2015.
 */
public class GPSListener extends AbstractSensorListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{

    private LocationRequest locationRequest;
    private GoogleApiClient apiClient;

    private Location currentLocation = null;

    private boolean connected = false;

    public GPSListener(Context context)
    {
        //configure Location Request
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //create API client
        Log.i("GPSListener", "locationRequest created");
        apiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        Log.i("GPSListener", "apiClient created");
        apiClient.connect();
    }

    @Override
    public String measure()
    {
        String returnValue = "";
        if(currentLocation != null)
        {
            returnValue += "\"latitude\" : \"" + currentLocation.getLatitude() + "\",\n";
            returnValue += "\"longitude\" : \"" + currentLocation.getLongitude() + "\",\n";
        }
        else
        {
            returnValue += "\"latitude\" : null,\n";
            returnValue += "\"longitude\" : null,\n";
        }
        if(currentLocation.hasSpeed())
        {
            returnValue += "\"speed\" : \"" + (currentLocation.getSpeed()*3.6) + "\",\n";
        }
        else
        {
            returnValue += "\"speed\" : null,\n";
        }
        if(currentLocation.hasAccuracy())
        {
            returnValue += "\"accuracy\" : \"" + currentLocation.getAccuracy() + "\"\n";
        }
        else
        {
            returnValue += "\"accuracy\" : null,\n";
        }
        returnValue += "}";
        return returnValue;
    }

    @Override
    public String getName()
    {
        return "\"gps\" : \n{\n";
    }

    @Override
    public void stopListener(SensorManager sensorManager)
    {
        if(connected) LocationServices.FusedLocationApi.removeLocationUpdates(apiClient,this);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        connected = true;
        Log.i("GPSListener", "connected");
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient,locationRequest,this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        currentLocation = location;
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i("GPSListener", "suspended " + String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.i("GPSListener", "connection failed!");
    }
}
