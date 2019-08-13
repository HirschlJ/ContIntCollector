package at.ac.univie.jakobhirschl.contintcollector.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import at.ac.univie.jakobhirschl.contintcollector.AbstractActivity;
import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;
import at.ac.univie.jakobhirschl.contintcollector.sensorlistener.NoiseListener;

public class SettingsActivity extends AbstractActivity
{

    private AppData appData;
    private NoiseListener noiseListener;
    private DBCalibrationTask dbCalibrationTask;
    private UITask uiTask;
    public EditText testValues;
    //Threadpool for repeatedly meassurement while testing db calibration
    ScheduledExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        appData = new AppData();
        try
        {
            appData.readData(this);
        }
        catch (Exception e)
        {
            Log.i("Settings","Reading Data Failed");
        }
        //Set all values to values from AppData
        CheckBox useGPS = (CheckBox)findViewById(R.id.useGPS);
        CheckBox useInteraction = (CheckBox)findViewById(R.id.useInteraction);
        CheckBox useLight = (CheckBox)findViewById(R.id.useLight);
        CheckBox useNoise = (CheckBox)findViewById(R.id.useNoise);
        CheckBox useAccelerometer = (CheckBox)findViewById(R.id.useAcc);
        CheckBox useBluetooth = (CheckBox)findViewById(R.id.useBluetooth);
        CheckBox smooth = (CheckBox)findViewById(R.id.smoothData);
        useGPS.setChecked(appData.useGPS);
        useInteraction.setChecked(appData.useInteraction);
        useLight.setChecked(appData.useLight);
        useNoise.setChecked(appData.useNoise);
        useAccelerometer.setChecked(appData.useAccelerometer);
        useBluetooth.setChecked(appData.useBluetooth);
        smooth.setChecked(appData.smooth);
        EditText referenceAmplField = (EditText)findViewById(R.id.referenceAmpl);
        referenceAmplField.setText(String.valueOf(appData.referenceAmpl));
        EditText interval = (EditText)findViewById(R.id.interval);
        EditText interpolation = (EditText)findViewById(R.id.interpolation);
        EditText minLonLat = (EditText)findViewById(R.id.minLonLat);
        interval.setText(String.valueOf(appData.interval/1000.0));
        interpolation.setText(String.valueOf(appData.interpolation));
        minLonLat.setText(String.valueOf(appData.minLonLat));
        testValues = (EditText)findViewById(R.id.testValues);
        testValues.setText("0");

        //create change listener for referenceAmplField
        interval.addTextChangedListener(new IntervalTextWatcher());
        referenceAmplField.addTextChangedListener(new RefAmplTextWatcher());
        interpolation.addTextChangedListener(new InterpolationTextWatcher());
        minLonLat.addTextChangedListener(new MinLongLatTextWatcher());

        //create Noise Listener vor Tests
        noiseListener = new NoiseListener(appData.referenceAmpl);
        noiseListener.stop();
        //create calibration Task
        dbCalibrationTask = new DBCalibrationTask(this);
        uiTask = new UITask();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            noiseListener.stop();
            executorService.shutdownNow();
        }
        catch (Exception e)
        {
            //ntd
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            noiseListener.stop();
            executorService.shutdownNow();
        }
        catch (Exception e)
        {
            //ntd
        }
    }

    private void serialize()
    {
        appData.serialize(this);
    }

    public void setUseGPS(View v)
    {
        appData.useGPS = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setUseInteraction(View v)
    {
        appData.useInteraction = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setUseLight(View v)
    {
        appData.useLight = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setUseNoise(View v)
    {
        appData.useNoise = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setUseAcc(View v)
    {
        appData.useAccelerometer = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setUseBluetooth(View v)
    {
        appData.useBluetooth = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void setSmooth(View v)
    {
        appData.smooth = ((CheckBox)v).isChecked();
        appData.serialize(this);
    }

    public void testButton(View v)
    {
        if(v.getTag()==null)
        {
            //start noise listener and the thread measuring every 0.5 seconds
            Log.i("Settings", "Start Decibel-Test");
            ((Button)v).setText("Stop Test");
            noiseListener.start();
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(dbCalibrationTask, 0, 500, TimeUnit.MILLISECONDS);
            v.setTag("Test running");
        }
        else
        {
            //stop calibration task and set the field to 0 after a short timeout
            Log.i("Settings", "Stop Decibel-Test");
            ((Button)v).setText("Test Calibration");
            noiseListener.stop();
            executorService.shutdownNow();
            v.setTag(null);
            try
            {
                Thread.sleep(600);
            }
            catch (InterruptedException e)
            {
                //ntd
            }
            testValues.setText("0");
        }
    }

    public void calibrationHelp(View v)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Decibel are calculated with the Formular:\n\n20*log10(ampl/refAmpl)" +
                "\n\nHere you can change and test the reference Amplitude" +
                "\n\nKeep in mind that your microphone may not be able to measure amplitudes louder than average voice!");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void accHelp(View v)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Accelerometer Data can be measured, but is not yet used in Visualisation" +
                ", since it is not reliable and significant enough. It is turned off by default.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void gpsHelp(View v)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS has to be activated. Network connection is recommended for more accurate results.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void minLonLatHelp(View v)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Minimum difference between two following measurements longitude or latitude to draw a point on the map.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private class DBCalibrationTask implements Runnable
    {
        //starts the task operating on UI every 0.5 seconds
        private SettingsActivity settingsActivity;
        public DBCalibrationTask(SettingsActivity settingsActivity)
        {
            this.settingsActivity = settingsActivity;
        }
        @Override
        public void run()
        {
            settingsActivity.runOnUiThread(uiTask);
        }
    }

    private class UITask implements Runnable
    {
        @Override
        public void run()
        {
            //measures from noise listener and sets the value
            String measuredValue = noiseListener.measure();
            if(measuredValue.length()>12)measuredValue = measuredValue.substring(0,11);
            testValues.setText(measuredValue);
        }
    }

    //some textWatchers to guarantee meaningful values in settings

    private class RefAmplTextWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            double newRef;
            try
            {
                 newRef = Double.parseDouble(s.toString());
                if(newRef==0) newRef = 0.0000000000000001;
            }
            catch (Exception e)
            {
                newRef  = 1.0;
            }
            appData.referenceAmpl = newRef;
            Log.i("Settings", "Set " + newRef);
            noiseListener.changeReferenceAmpl(newRef);
            serialize();
        }
    }

    private class IntervalTextWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            double newInterval;
            try
            {
                newInterval = Double.parseDouble(s.toString());
                if(newInterval<0.001) newInterval = 0.001;
            }
            catch (Exception e)
            {
                newInterval  = 1.0;
            }
            appData.interval = (long)newInterval*1000;
            Log.i("Settings", "Set Interval" + newInterval);
            serialize();
        }
    }

    private class InterpolationTextWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            int newInterpolation;
            try
            {
                newInterpolation = Integer.parseInt(s.toString());
            }
            catch (Exception e)
            {
                newInterpolation  = 10;
            }
            appData.interpolation = newInterpolation;
            Log.i("Settings", "Set Interpolation" + newInterpolation);
            serialize();
        }
    }

    private class MinLongLatTextWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            double newMinLonLat;
            try
            {
                newMinLonLat = Double.parseDouble(s.toString());
                if(newMinLonLat==0) newMinLonLat = 0.00001;
            }
            catch (Exception e)
            {
                newMinLonLat = 0.00001;
            }
            appData.minLonLat = newMinLonLat;
            Log.i("Settings", "Set minLonLat" + newMinLonLat);
            serialize();
        }
    }


    //reset database if alert is confirmed
    public void resetDatabase(View v)
    {
        final DBHandler dbHandler = new DBHandler(this);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("This action will delete ALL your Session Data!");
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton("OK, I want to delete \nALL Session Data", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dbHandler.reset();
            }
        });

        alertDialogBuilder.setNegativeButton("CANCEL", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void keyboardAway(View v)
    {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 1);
    }
}
