package at.ac.univie.jakobhirschl.contintcollector.vis;

import android.app.AlertDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.data.DataSingleton;
import at.ac.univie.jakobhirschl.contintcollector.data.GPSData;
import at.ac.univie.jakobhirschl.contintcollector.data.SessionData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;

public class RatioDiagram extends Fragment implements View.OnTouchListener
{

    private static final String ARG_PARAM1 = "param1";

    int ID;
    XYPlot plot;
    private PointF minXY;
    private PointF maxXY;
    float borderLeft;
    float borderRight;
    DataSingleton dataSingleton;
    SessionData sessionData;
    List<String> usedSensorNames;
    String selected1;
    String selected2;

    private float lastScrolling = -1;
    private float distBetweenFingers = -1;
    private float lastZooming;
    private boolean zoom = false;

    public static RatioDiagram newInstance(int ID)
    {
        RatioDiagram fragment = new RatioDiagram();
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
        View fragmentView = inflater.inflate(R.layout.fragment_ratio_diagram, container, false);
        plot = (XYPlot) fragmentView.findViewById(R.id.ratioXYPlot);
        plot.setOnTouchListener(this);

        Log.i("RatioDiagram", "Ratiodiagram for Session " + ID);

        dataSingleton = DataSingleton.getInstance();
        dataSingleton.setContext(getActivity());
        sessionData = dataSingleton.getSessionData(ID);

        //create both spinners
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
            usedSensorNames.add("Speed");
        }
        if(sessionData.isUseLight())
        {
            usedSensorNames.add("Light");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, usedSensorNames.toArray(new String[usedSensorNames.size()]));

        Spinner spinner1 = (Spinner)fragmentView.findViewById(R.id.spinner);
        spinner1.setAdapter(adapter);
        spinner1.setOnItemSelectedListener(new ChangeListener1());
        Spinner spinner2 = (Spinner)fragmentView.findViewById(R.id.spinner2);
        spinner2.setAdapter(adapter);
        spinner2.setOnItemSelectedListener(new ChangeListener2());
        //set them to default
        if(usedSensorNames.size()>0) selected1=selected2= usedSensorNames.get(0);
        //visualize
        visualizeSelected();

        return fragmentView;
    }

    private void visualizeSelected()
    {
        plot.clear();
        //get data from selected sensors
        List<Double> values1 = getDoubleListByName(selected1);
        List<Double> values2 = getDoubleListByName(selected2);
        Map<Double,Double> map = new HashMap<>();
        Map<Double,Double> numVals = new HashMap<>();
        for(int i = 0; i < values1.size(); i++)
        {
            //check if speed value is missing. those data sets cannot be used
            if(!((selected1.equals("Speed") || selected2.equals("Speed"))&&((values1.get(i)==-1.0) || (values2.get(i) == -1.0))))
            {
                //sum up all values from sensor 2 and count their number
                if(map.containsKey(values2.get(i)))
                {
                    map.put(values2.get(i), map.get(values2.get(i))+values1.get(i));
                    if(numVals.containsKey(values2.get(i)))
                    {
                        numVals.put(values2.get(i),numVals.get(values2.get(i))+1.0);
                    }
                    else numVals.put(values2.get(i),2.0);
                }
                else map.put(values2.get(i),values1.get(i));
            }
        }
        SortedSet<Double> sortedSet = new TreeSet<>(map.keySet());
        List<Double> sortedValues1 = new ArrayList<>();
        List<Double> sortedValues2 = new ArrayList<>();
        //divide summed up values through there number to get the average
        for(Double key : sortedSet)
        {
            if(numVals.containsKey(key))
            {
                sortedValues1.add(map.get(key)/numVals.get(key));
            }
            else sortedValues1.add(map.get(key));
            sortedValues2.add(key);
        }
        //check if diagram can be created
        if(sortedValues2.size()<2)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage(selected2 + " has no differing values. \nRatio diagram cannot be created!");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("OK", null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        //create diagram
        XYSeries ratioSeries = new SimpleXYSeries(sortedValues2, sortedValues1, "Ratio avg(" + selected1 + "):" + selected2);
        LineAndPointFormatter ratioSeriesFormat = new LineAndPointFormatter(Color.GREEN, Color.GREEN, null, null);
        plot.addSeries(ratioSeries, ratioSeriesFormat);
        //add legend
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.06f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.3f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.01f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }
        else
        {
            plot.getLegendWidget().setSize(new SizeMetrics(0.12f, SizeLayoutType.RELATIVE, 0.9f, SizeLayoutType.RELATIVE));
            plot.getLegendWidget().position(0.6f, XLayoutStyle.RELATIVE_TO_RIGHT, 0.01f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.RIGHT_BOTTOM);
        }
        plot.setTitle("Ratio Diagram for Session " + ID);
        plot.setDomainLabel(selected2);
        plot.setRangeLabel("avg(" + selected1 + ")");
        plot.setRangeBoundaries(0, 1, BoundaryMode.AUTO);
        plot.setDomainBoundaries(0,1,BoundaryMode.AUTO);
        plot.redraw();
        //Set of internal variables for keeping track of the boundaries
        plot.calculateMinMaxVals();
        minXY=new PointF(plot.getCalculatedMinX().floatValue(),plot.getCalculatedMinY().floatValue());
        maxXY=new PointF(plot.getCalculatedMaxX().floatValue(),plot.getCalculatedMaxY().floatValue());
    }

    private List<Double> getDoubleListByName(String name)
    {
        Log.i("RatioDiagram","get data " + name);
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
        if(name.equals("Speed"))
        {
            List<GPSData> gpsData = sessionData.getGpsData();
            for(GPSData data : gpsData)
            {
                list.add(data.getSpeed());
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

    //Functions for moving and zooming the diagram.

    @Override
    public boolean onTouch(View arg0, MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_MOVE)
        {
            // if two fingers are used -> zoom
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
            //if one finger was used -Y scroll
            if(event.getPointerCount()==1 && !zoom)
            {
                if(lastScrolling==-1) lastScrolling = event.getX();
                float dist = lastScrolling - event.getX();
                lastScrolling = event.getX();
                scroll(dist);
            }
        }
        //reset values if action stopped
        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            distBetweenFingers = -1;
            lastScrolling = -1;
            zoom = false;
        }
        return true;
    }


    //zoom according to scale (distance from function scale) and mid point of zoom
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

    //move diagrams min and max xy and redraw (pan = distance from event)
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

    //get centre of motion event
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

    //calculate distance of motion event
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

    //ChangeListener for Scrollbar

    private class ChangeListener1 implements AdapterView.OnItemSelectedListener
    {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            selected1 = usedSensorNames.get(position);
            visualizeSelected();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    }

    private class ChangeListener2 implements AdapterView.OnItemSelectedListener
    {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            selected2 = usedSensorNames.get(position);
            visualizeSelected();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    }

}
