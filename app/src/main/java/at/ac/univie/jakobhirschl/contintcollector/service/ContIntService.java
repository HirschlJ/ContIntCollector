package at.ac.univie.jakobhirschl.contintcollector.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.view.View.OnTouchListener;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.AbstractSensorListener;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.AccelerometerListener;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.BluetoothListener;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.GPSListener;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.LightListener;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.NoiseListener;

/**
 * Created by Jakobus on 03.11.2015.
 */
public class ContIntService extends Service implements OnTouchListener
{
    private String serviceName = this.getClass().getName();

    //layout to detect touch events
    private LinearLayout touchDetectionLayout;
    private WindowManager windowManager;

    //Threadpool for repeatedly meassurement
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    //List with SensorListeners
    private ArrayList<AbstractSensorListener> sensorListeners;

    //Sensor Manager
    private SensorManager sensorManager;

    //booleans
    private boolean[] sensorsToUse;

    //Interactions
    private long numOfInteractions;
    private long intSinceLastMeasurement;

    //DBHandler
    private DBHandler dbHandler;

    //session
    private int session;

    //WAKE_LOCK
    private PowerManager.WakeLock wakeLock;

    //interval
    private long interval;

    @Override
    public void onCreate()
    {
        super.onCreate();
        //create invisible global touchListener
        touchDetectionLayout = new LinearLayout(this);
        LayoutParams params = new LayoutParams(1,1);
        touchDetectionLayout.setLayoutParams(params);
        touchDetectionLayout.setOnTouchListener(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams(
                1,1,
                WindowManager.LayoutParams.TYPE_TOAST, // Type Toast: like a transient notification
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Get notification when action happens outside
                PixelFormat.TRANSLUCENT);
        windowManager.addView(touchDetectionLayout, windowManagerParams);
        numOfInteractions = 0;
        intSinceLastMeasurement = 0;

        //init Listener Array
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorListeners = new ArrayList<>();

        //get Database handler
        dbHandler = new DBHandler(this);

        //WAKE_LOCK -> CPU needs to stay active
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPU");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //init requested listeners
        session = intent.getIntExtra(getResources().getString(R.string.IntentSession),1);
        double referenceAmpl = intent.getDoubleExtra(getResources().getString(R.string.IntentExtraDB), 1.0);
        interval = intent.getLongExtra(getResources().getString(R.string.IntentExtraInterval),1000);

        //start thread
        Log.i(serviceName,"Interval: " + interval + "ms");
        executorService.scheduleAtFixedRate(new ContIntServiceTask(), 10000, interval, TimeUnit.MILLISECONDS);
        Log.i(serviceName, "Service started");

        //create listeners
        sensorsToUse = intent.getBooleanArrayExtra(getResources().getString(R.string.IntentExtra));
        if(sensorsToUse[0]) sensorListeners.add(new GPSListener(this));
        if(sensorsToUse[2]) sensorListeners.add(new NoiseListener(referenceAmpl));
        if(sensorsToUse[3])sensorListeners.add(new LightListener(sensorManager));
        if(sensorsToUse[4])sensorListeners.add(new AccelerometerListener(sensorManager));
        if(sensorsToUse[5]) sensorListeners.add(new BluetoothListener(this));
        return 1;
    }

    private class ContIntServiceTask implements Runnable
    {
        @Override
        public void run()
        {
            // save data
            String data = "{\n";
            long timestamp = System.currentTimeMillis();
            data += "\"timestamp\" : \"" + timestamp + "\",\n";
            for(AbstractSensorListener listener : sensorListeners)
            {
                data+=listener.getName();
                data+=listener.measure();
                data+=",\n";
            }
            //get interactions
            if(sensorsToUse[1])
            {
                int interactions = (int) (numOfInteractions - intSinceLastMeasurement);
                intSinceLastMeasurement = numOfInteractions;
                String interactionData = String.valueOf(interactions);
                data += "\"interaction\" :\n{\n";
                data += "\"touch\" : \"" + interactionData + "\"\n";
                data += "},\n";
            }
            data = data.substring(0,data.length()-2);
            data+="\n}";
            //Log.i("test",data);
            dbHandler.addData(session,timestamp,data);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE && sensorsToUse[1])
        {
            numOfInteractions++;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        //if window was created remove view
        if(windowManager!=null)
            if(touchDetectionLayout!=null) windowManager.removeView(touchDetectionLayout);
        //shutdown thread
        executorService.shutdownNow();
        //unregister SensorListeners
        for(AbstractSensorListener listener : sensorListeners) listener.stopListener(sensorManager);
        sensorListeners.clear();
        wakeLock.release();
        Log.i(serviceName,"Service stopped");
        super.onDestroy();
    }
}
