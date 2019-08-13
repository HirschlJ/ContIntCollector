package at.ac.univie.jakobhirschl.contintcollector.vis;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.data.DataSingleton;
import at.ac.univie.jakobhirschl.contintcollector.data.GPSData;
import at.ac.univie.jakobhirschl.contintcollector.data.SessionData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;

public class XYDiagram extends Fragment implements View.OnTouchListener
{

    private static final String ARG_PARAM1 = "param1";

    private XYPlot plot;
    private PointF minXY;
    private PointF maxXY;
    private float borderLeft;
    private float borderRight;
    private DataSingleton dataSingleton;
    private SessionData sessionData;
    private int ID;

    private float lastScrolling = -1;
    private float distBetweenFingers = -1;
    private float lastZooming;
    private boolean zoom = false;

    public static XYDiagram newInstance(int ID)
    {
        XYDiagram fragment = new XYDiagram();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, ID);
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle getBundle(int ID)
    {
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, ID);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            ID = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_xydiagram, container, false);
        plot = (XYPlot)fragmentView.findViewById(R.id.simpleXYDiagram);
        //set default min/max for sensors
        double maxSpeed = 9.0;
        double minDec = 20.0;
        double maxDec = 69.0;
        double minLight  = 1000.0;
        double maxLight = 49.0;
        double maxBT = 5;
        double maxInt = 5;

        // initialize our XYPlot reference:
        plot.setOnTouchListener(this);
        Log.i("XYDiagram", "XYDiagram for Session " + ID);

        // get data
        dataSingleton = DataSingleton.getInstance();
        dataSingleton.setContext(getActivity());
        sessionData = dataSingleton.getSessionData(ID);

        //get time span
        List<Double> timeSpan = sessionData.getTimeSpan();

        //if interaction is used -> determine min and max interaction and create line
        if(sessionData.isUseInteraction())
        {
            List<Integer> interationData = sessionData.getIntData();
            List<Double> list = new ArrayList<>();
            for(int data : interationData)
            {
                if(data>maxInt) maxInt = data;
            }
            for(int data : interationData)
            {
                list.add(((double)data)/maxInt);
            }
            XYSeries intSeries = new SimpleXYSeries(timeSpan, list,"Int/sec(" + 0 + "-" + (int)maxInt + ")" );
            LineAndPointFormatter intSeriesFormat = new LineAndPointFormatter(Color.GREEN,Color.GREEN,null,null);
            plot.addSeries(intSeries,intSeriesFormat);
        }
        //if bluetooth is used -> determine min and max bt and create line
        if(sessionData.isUseBluetooth())
        {
            List<Integer> bluetoothData = sessionData.getBluetoothData();
            List<Double> list = new ArrayList<>();
            for(int data : bluetoothData)
            {
                if(data>maxBT) maxBT = data;
            }
            for(int data : bluetoothData)
            {
                list.add(((double)data)/maxBT);
            }
            XYSeries btSeries = new SimpleXYSeries(timeSpan, list,"BT devices(" + 0 + "-" + maxBT + ")");
            LineAndPointFormatter btSeriesFormat = new LineAndPointFormatter(Color.BLUE,Color.BLUE,null,null);
            plot.addSeries(btSeries,btSeriesFormat);
        }
        //if noise is used -> determine min and max noise and create line
        if(sessionData.isUseNoise())
        {
            List<Double> noiseData = sessionData.getNoiseData();
            List<Double> list = new ArrayList<>();
            for(double data : noiseData)
            {
                if(data>maxDec) maxDec = data;
                if(data<minDec) minDec = data;
            }
            for(double data : noiseData)
            {
                list.add((data-minDec)/(maxDec-minDec));
            }
            XYSeries noiseSeries = new SimpleXYSeries(timeSpan, list,"dB(" + (int)(minDec) + "-" + (int)(maxDec+1) + ")");
            LineAndPointFormatter noiseSeriesFormat = new LineAndPointFormatter(Color.MAGENTA,Color.MAGENTA,null,null);
            plot.addSeries(noiseSeries,noiseSeriesFormat);
        }
        //if gps is used -> determine min and max speed and create line
        if(sessionData.isUseGPS())
        {
            List<GPSData> gpsData = sessionData.getGpsData();
            List<Double> list = new ArrayList<>();
            List<Double> time = new ArrayList<>();
            for(GPSData data : gpsData)
            {
                if(data.getSpeed()>maxSpeed) maxSpeed = data.getSpeed();
            }
            int i = 0;
            for(GPSData data : gpsData)
            {
                //if speed was measured -> add speed and time to list
                if(data.getSpeed()!=-1.0)
                {
                    list.add(data.getSpeed()/maxSpeed);
                    time.add(timeSpan.get(i));
                }
                i++;
            }
            XYSeries speedSeries = new SimpleXYSeries(time, list,"Speed(" + 0 + "-" + (int)(maxSpeed+1) + ")(km/h)");
            LineAndPointFormatter speedSeriesFormat = new LineAndPointFormatter(Color.RED,Color.RED,null,null);
            plot.addSeries(speedSeries,speedSeriesFormat);
        }
        //if light is used -> determine min and max light and create line
        if(sessionData.isUseLight())
        {
            List<Double> lightData = sessionData.getLightData();
            List<Double> list = new ArrayList<>();
            for(double data : lightData)
            {
                if(data>maxLight) maxLight = data;
                if(data<minLight) minLight = data;
            }
            for(double data : lightData)
            {
                list.add((data-minLight)/(maxLight-minLight));
            }
            XYSeries lightSeries = new SimpleXYSeries(timeSpan, list,"Light(" + (int)(minLight) + "-" + (int)(maxLight+1) + ")");
            LineAndPointFormatter lightSeriesFormat = new LineAndPointFormatter(Color.YELLOW,Color.YELLOW,null,null);
            plot.addSeries(lightSeries,lightSeriesFormat);
        }
        //create legend
        plot.getLegendWidget().setTableModel(new DynamicTableModel(3, 3));
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.06f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.1f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.015f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }
        else
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.12f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.1f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.023f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }

        plot.setTitle("XYDiagram for Session " + ID);
        plot.setDomainBoundaries(0, 1, BoundaryMode.AUTO);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
        plot.redraw();

        //Set of internal variables for keeping track of the boundaries
        plot.calculateMinMaxVals();
        minXY=new PointF(plot.getCalculatedMinX().floatValue(),plot.getCalculatedMinY().floatValue());
        maxXY=new PointF(plot.getCalculatedMaxX().floatValue(),plot.getCalculatedMaxY().floatValue());
        maxXY=new PointF(plot.getCalculatedMaxX().floatValue(),plot.getCalculatedMaxY().floatValue());
        return fragmentView;
    }

    //Functions for moving and zooming the diagram. (see RatioDiagram.java)

    @Override
    public boolean onTouch(View arg0, MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_MOVE)
        {
            if(event.getPointerCount()==2)
            {
                zoom = true;
                RectF rectF = plot.getGraphWidget().getGridRect();
                borderLeft = rectF.left-30;
                borderRight = rectF.right-30;
                if(event.getX(1)<borderLeft) return true;
                if(distBetweenFingers==-1) distBetweenFingers = spacing(event);
                float oldDist = distBetweenFingers;
                distBetweenFingers = spacing(event);
                lastZooming  = oldDist/distBetweenFingers;
                float midPoint = getZoomMidPoint(event);
                zoom(lastZooming, midPoint);
            }
            if(event.getPointerCount()==1 && !zoom)
            {
                if(lastScrolling==-1) lastScrolling = event.getX();
                float dist = lastScrolling - event.getX();
                lastScrolling = event.getX();
                scroll(dist);
            }
        }
        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            distBetweenFingers = -1;
            lastScrolling = -1;
            zoom = false;
        }
        return true;
    }

    private void zoom(float scale, float midPoint)
    {
        float domainSpan = maxXY.x - minXY.x;
        float domainMidPoint = maxXY.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;
        float rate;
        if(scale < 1) rate = 1 - scale;
        else rate = (scale-1)*-1;
        float diff = (midPoint - domainMidPoint)*rate;
        minXY.x=domainMidPoint - offset + diff;
        maxXY.x=domainMidPoint + offset + diff;
        plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
        plot.redraw();
    }

    private void scroll(float pan)
    {
        float domainSpan = maxXY.x - minXY.x;
        float step = domainSpan / plot.getWidth();
        float offset = pan * step;
        minXY.x+= offset;
        maxXY.x+= offset;
        plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
        plot.redraw();
    }

    private float getZoomMidPoint(MotionEvent event)
    {
        float x;
        float width = borderRight-borderLeft + 60;
        try
        {
            x = (maxXY.x - minXY.x)*(((event.getX(0) + event.getX(1))/2.0f-borderLeft) / width)+minXY.x;
        }
        catch(Exception e)
        {
            x = 1;
        }
        return x;
    }

    private float spacing(MotionEvent event)
    {
        float x;
        float y;
        try
        {
            x = event.getX(0) - event.getX(1);
            y = event.getY(0) - event.getY(1);
        }
        catch(Exception e)
        {
            x = 1;
            y = 1;
        }
        return (float)Math.sqrt(x * x + y * y);
    }

}
