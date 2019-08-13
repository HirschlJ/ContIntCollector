package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Jakob Hirschl on 19.11.2015.
 */
public class BluetoothListener extends AbstractSensorListener
{
    private BluetoothAdapter adapter;
    int numDevices = 0;
    BluetoothBroadcastReceiver bluetoothBroadcastReceiver;
    Context context;

    public BluetoothListener(Context context)
    {
        //get bluetooth adapter and activate if it is not
        adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled()) adapter.enable();

        //create filter for actions
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //register Receiver
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();
        context.registerReceiver(bluetoothBroadcastReceiver, filter);
        this.context = context;
        //start bluetooth discovery
        adapter.startDiscovery();
    }

    @Override
    public String measure()
    {
        //start discovery, if it is not started yet
        if(!adapter.isDiscovering()) adapter.startDiscovery();
        String returnValue = "";
        returnValue += "\"numOfBTDevices\" : \""+ numDevices + "\"\n";
        returnValue += "}";
        return returnValue;
    }

    @Override
    public String getName()
    {
        return "\"bluetooth\" : \n{\n";
    }

    @Override
    public void stopListener(SensorManager sensorManager)
    {
        if(adapter.isEnabled()) adapter.disable();
        context.unregisterReceiver(bluetoothBroadcastReceiver);
    }

    private class BluetoothBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.i("BT",action);
            //if ACTION_Fount -> summ up numDevices
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                numDevices++;
            }
            //if Discovery was finished reset numDevices
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                numDevices = 0;
            }
        }
    }

}
