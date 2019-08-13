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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.androidplot.ui.AnchorPosition;
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
import java.util.Collections;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.data.DataSingleton;
import at.ac.univie.jakobhirschl.contintcollector.data.GPSData;
import at.ac.univie.jakobhirschl.contintcollector.data.SessionData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;

public class SingleSensorDiagram extends Fragment implements View.OnTouchListener
{

    private static final String ARG_PARAM1 = "param1";

    private int ID;
    private XYPlot plot;
    private PointF minXY;
    private PointF maxXY;
    private float borderLeft;
    private float borderRight;
    private DataSingleton dataSingleton;
    private SessionData sessionData;
    private List<String> usedSensorNames;
    private String selected;
    private List<Double> time = new ArrayList<>();

    private float lastScrolling = -1;
    private float distBetweenFingers = -1;
    private float lastZooming;
    private boolean zoom = false;

    public static SingleSensorDiagram newInstance(int ID)
    {
        SingleSensorDiagram fragment = new SingleSensorDiagram();
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
        View fragmentView = inflater.inflate(R.layout.fragment_single_sensor_diagram, container, false);
        // initialize our XYPlot reference:
        plot = (XYPlot) fragmentView.findViewById(R.id.singeSensorPlot);
        plot.setOnTouchListener(this);

        Log.i("RatioDiagram", "XYDiagram for Session " + ID);

        //get data
        dataSingleton = DataSingleton.getInstance();
        dataSingleton.setContext(getActivity());
        sessionData = dataSingleton.getSessionData(ID);

        //create spinner
        usedSensorNames = new ArrayList<>();

        if(sessionData.isUseInteraction())
        {
            usedSensorNames.add("Interaction");
        }
        if(sessionData.isUseBluetooth())
        {
            usedSensorNames.add("Bluetooth");
        }
        if(sessionData.isUseNoise())
        {
            usedSensorNames.add("Noise");
        }
        if(sessionData.isUseGPS())
        {
            usedSensorNames.add("Speed(km/h)");
        }
        if(sessionData.isUseLight())
        {
            usedSensorNames.add("Light");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, usedSensorNames.toArray(new String[usedSensorNames.size()]));

        Spinner spinner = (Spinner)fragmentView.findViewById(R.id.spinner3);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new ChangeListener());
        if(usedSensorNames.size()>0) selected = usedSensorNames.get(0);

        visualizeSelected();

        return fragmentView;
    }

    private void visualizeSelected()
    {
        plot.clear();
        //get data
        List<Double> values = getDoubleListByName(selected);
        //create diagram
        XYSeries singleSeries = new SimpleXYSeries(time, values, selected);
        LineAndPointFormatter singleSeriesFormat = new LineAndPointFormatter(Color.GREEN, Color.GREEN, null, null);
        plot.addSeries(singleSeries, singleSeriesFormat);
        //add legend
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.06f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.5f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.01f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }
        else
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.12f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.5f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.01f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }
        plot.setTitle("XYDiagram for Session " + ID);
        plot.setRangeLabel(selected);
        if(values.size() > 0)
        {
            if (Collections.max(values) == 0.0) plot.setRangeBoundaries(0, 0.5, BoundaryMode.FIXED);
            else plot.setRangeBoundaries(0, 1, BoundaryMode.AUTO);
            plot.setDomainBoundaries(0, 1, BoundaryMode.AUTO);
        }
        plot.redraw();
        //Set of internal variables for keeping track of the boundaries
        plot.calculateMinMaxVals();
        minXY=new PointF(plot.getCalculatedMinX().floatValue(),plot.getCalculatedMinY().floatValue());
        maxXY=new PointF(plot.getCalculatedMaxX().floatValue(),plot.getCalculatedMaxY().floatValue());
    }

    private List<Double> getDoubleListByName(String name)
    {
        Log.i("RatioDiagram", "get data " + name);
        //only speed data can be missing, so time is the whole time span if name!=Speed
        if(!name.equals("Speed(km/h)")) time = sessionData.getTimeSpan();
        List<Double> list = new ArrayList<>();
        if(name.equals("Interaction"))
        {
            List<Integer> interationData = sessionData.getIntData();
            for(int data : interationData)
            {
                list.add((double)data);
            }
        }
        if(name.equals("Bluetooth"))
        {
            List<Integer> bluetoothData = sessionData.getBluetoothData();
            for(int data : bluetoothData)
            {
                list.add((double)data);
            }
        }
        if(name.equals("Noise"))
        {
            List<Double> noiseData = sessionData.getNoiseData();
            for(double data : noiseData)
            {
                list.add(data);
            }
        }
        if(name.equals("Speed(km/h)"))
        {
            List<Double> allTime = sessionData.getTimeSpan();
            time.clear();
            List<GPSData> gpsData = sessionData.getGpsData();
            int i = 0;
            for(GPSData data : gpsData)
            {
                //if speed was measured -> add speed and time to list
                if(data.getSpeed()!=-1.0)
                {
                    list.add(data.getSpeed());
                    time.add(allTime.get(i));
                }
                i++;
            }
        }
        if(name.equals("Light")) {
            List<Double> lightData = sessionData.getLightData();
            for(double data : lightData)
            {
                list.add(data);
            }
        }
        return list;
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

    private class ChangeListener implements AdapterView.OnItemSelectedListener
    {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            selected = usedSensorNames.get(position);
            visualizeSelected();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    }

}
