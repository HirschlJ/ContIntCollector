package at.ac.univie.jakobhirschl.contintcollector.sensorlistener;

import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by Jakobus on 14.11.2015.
 */
public class NoiseListener extends AbstractSensorListener
{
    private double referenceAmpl;
    private MediaRecorder mediaRecorder;
    private int latestAccuracy;

    public NoiseListener(double referenceAmpl)
    {
        //setup, prepare and start media recorder
        this.referenceAmpl = referenceAmpl;
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setOutputFile("/dev/null");
        try
        {
            mediaRecorder.prepare();
            mediaRecorder.start();
            measure();
        }
        catch(Exception e)
        {
            Log.i("NoiseListener", "NoiseListener failed");
        }
    }

    public void start()
    {
        //setup, prepare and start media recorder
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setOutputFile("/dev/null");
        try
        {
            mediaRecorder.prepare();
            mediaRecorder.start();
            measure();
        }
        catch(Exception e)
        {
            Log.i("NoiseListener", "NoiseListener failed");
        }
    }

    public void stop()
    {
        //stop and reset media recorder
        try
        {
            mediaRecorder.stop();
            mediaRecorder.reset();
        }
        catch (Exception e)
        {
            //ntd
        }
    }

    public void changeReferenceAmpl(double newRef)
    {
        referenceAmpl = newRef;
    }

    @Override
    public String measure()
    {
        double aplitude = mediaRecorder.getMaxAmplitude();
        //calculate db using the reference aplitude from settings
        double dB = 20 * Math.log10(aplitude/referenceAmpl);
        String returnValue = "";
        returnValue += "\"dB\" : \""+ dB + "\"\n";
        returnValue += "}";
        return returnValue;
    }

    @Override
    public String getName()
    {
        return "\"noise\" : \n{\n";
    }

    @Override
    public void stopListener(SensorManager sensorManager)
    {
        stop();
    }
}
