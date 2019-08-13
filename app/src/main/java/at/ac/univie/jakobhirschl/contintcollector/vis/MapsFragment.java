package at.ac.univie.jakobhirschl.contintcollector.vis;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;
import at.ac.univie.jakobhirschl.contintcollector.data.DataSingleton;
import at.ac.univie.jakobhirschl.contintcollector.data.GPSData;
import at.ac.univie.jakobhirschl.contintcollector.data.SessionData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;
import at.ac.univie.jakobhirschl.contintcollector.selection.TabVisSelection;

public class MapsFragment extends Fragment
{
    private static final String ARG_PARAM1 = "param1";

    private MapView mapView;
    private GoogleMap mMap;

    private int ID;
    private DataSingleton dataSingleton;
    private SessionData sessionData;
    private List<String> usedSensorNames;

    private TextView text1;
    private TextView text2;
    private TextView text3;
    private Canvas canvas;

    private List<GPSData> gpsData = new ArrayList<>();
    private List<Double> speedData = new ArrayList<>();
    private List<Double> lightData = new ArrayList<>();
    private List<Double> noiseData = new ArrayList<>();
    private List<Integer> intData = new ArrayList<>();
    private List<Double> bluetoothData = new ArrayList<>();

    private ScheduledExecutorService executorService;

    private String selected;

    AppData appData;

    public static MapsFragment newInstance(int ID)
    {
        MapsFragment fragment = new MapsFragment();
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
        View fragmentView = inflater.inflate(R.layout.fragment_maps, container, false);

        appData = new AppData();
        try
        {
            appData.readData(getActivity());
        }
        catch(Exception e)
        {
            //ntd
        }

        try
        {
            MapsInitializer.initialize(getActivity());
        }
        catch (Exception e)
        {
            Log.e("MapFragment", "Could not initialize google play", e);
        }

        //get network status
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        //get data
        dataSingleton = DataSingleton.getInstance();
        dataSingleton.setContext(getActivity());
        sessionData = dataSingleton.getSessionData(ID);
        List<GPSData> gpsData = sessionData.getGpsData();
        List<Double> lightData = sessionData.getLightData();
        List<Double> noiseData = sessionData.getNoiseData();
        List<Integer> intData = sessionData.getIntData();
        List<Integer> bluetoothData = sessionData.getBluetoothData();

        boolean first = true;
        double lastLat=0.0;
        double lastLon=0.0;
        for(int i = 0; i < gpsData.size(); i++)
        {
            //if first or next is far away enough set data, else sum up data
            if(first || (Math.abs(gpsData.get(i).getLatitude()-lastLat) > appData.minLonLat) || (Math.abs(gpsData.get(i).getLongitude()-lastLon) > appData.minLonLat))
            {
                if(gpsData.get(i).getLatitude()!=-1.0 && gpsData.get(i).getLongitude()!=-1.0)
                {
                    this.gpsData.add(gpsData.get(i));
                    lastLat = gpsData.get(i).getLatitude();
                    lastLon = gpsData.get(i).getLongitude();
                    first = false;
                    try
                    {
                        if(this.gpsData.get(this.gpsData.size()-1).getSpeed()==-1.0) this.gpsData.get(this.gpsData.size()-1).setSpeed(this.gpsData.get(this.gpsData.size()-2).getSpeed());
                    }
                    catch (Exception e)
                    {
                        this.gpsData.get(this.gpsData.size()-1).setSpeed(0.0);
                    }
                    this.speedData.add(this.gpsData.get(this.gpsData.size()-1).getSpeed());
                    if(sessionData.isUseInteraction())
                    {
                        this.intData.add(intData.get(i));
                    }
                    if(sessionData.isUseBluetooth())
                    {
                        this.bluetoothData.add((double)bluetoothData.get(i));
                    }
                    if(sessionData.isUseNoise())
                    {
                        this.noiseData.add(noiseData.get(i));
                    }
                    if(sessionData.isUseLight())
                    {
                        this.lightData.add(lightData.get(i));
                    }
                }
            }
            else if(gpsData.get(i).getLatitude()!=-1.0 && gpsData.get(i).getLongitude()!=-1.0)
            {
                this.intData.set(this.intData.size()-1,this.intData.get(this.intData.size()-1)+intData.get(i));
            }
        }

        //add seonsors to spinner
        usedSensorNames = new ArrayList<>();

        if(sessionData.isUseGPS())
        {
            usedSensorNames.add("Speed");
        }
        if(sessionData.isUseBluetooth())
        {
            usedSensorNames.add("Bluetooth");
        }
        if(sessionData.isUseNoise())
        {
            usedSensorNames.add("Noise");
        }
        if(sessionData.isUseLight())
        {
            usedSensorNames.add("Light");
        }

        selected = "Speed";

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, usedSensorNames.toArray(new String[usedSensorNames.size()]));

        Spinner spinner = (Spinner)fragmentView.findViewById(R.id.spinner4);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new ChangeListener());

        if(networkInfo==null)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Network connection is required!");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("OK", null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        mapView = (MapView)fragmentView.findViewById(R.id.map);
        text1 = (TextView)fragmentView.findViewById(R.id.textView6);
        text2 = (TextView)fragmentView.findViewById(R.id.textView7);
        text3 = (TextView)fragmentView.findViewById(R.id.textView8);
        mapView.onCreate(savedInstanceState);

        //add map legend
        text2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    @Override
                    public void onGlobalLayout() {
                        MapLegend mapLegend = new MapLegend(getActivity(), (int) (text2.getY() + (text2.getHeight()/2)), (int) (text1.getY() + (text1.getHeight()/2)), (int) text2.getX() + text2.getWidth() + 20);
                        //MapLegend mapLegend = new MapLegend(getActivity(),getView().getHeight()- 50,getView().getHeight()-150, getView().getHeight()-50);
                        mapView.addView(mapLegend);
                        text2.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });



        if(mapView!=null)
        {
            mMap = mapView.getMap();
            mapReady();
        }

        return fragmentView;
    }

    public void mapReady()
    {
        //clear map
        mMap.clear();
        List<Double> list = getDoubleListByName(selected);
        //get min max and ratio of color
        double min = Collections.min(list);
        double max = Collections.max(list);
        double avg = ((max-min)/2.0)+min;
        double ratio;
        if(max!= min) ratio = 255/max*2;
        else ratio = 255;

        //set start point
        LatLng start = new LatLng(gpsData.get(0).getLatitude(), gpsData.get(0).getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, (mMap.getMaxZoomLevel() - 3.0f)));
        mMap.addMarker(new MarkerOptions().position(start).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title("Start"));

        //set end point
        LatLng end = new LatLng(gpsData.get(gpsData.size()-1).getLatitude(), gpsData.get(gpsData.size()-1).getLongitude());
        mMap.addMarker(new MarkerOptions().position(end).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("End"));

        // ad Interactions text to legend
        if(sessionData.isUseInteraction())
        {
            text3.setText("Interaction: " + Collections.min(intData) + "-" + Collections.max(intData));
        }
        else text3.setText("");
        text1.setText(max + "");
        text2.setText(min + "");

        //start thread to draw the route
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(new MapCreateThread(list,min,ratio,avg),1000, TimeUnit.MILLISECONDS);

        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    private class MapCreateThread implements Runnable
    {
        private List<Double> list;
        private double min;
        private double ratio;
        private double avg;

        public MapCreateThread(List<Double> list, double min, double ratio, double avg)
        {
            this.list = list;
            this.min = min;
            this.ratio = ratio;
            this.avg = avg;
        }

        @Override
        public void run()
        {
            //draw polyline between each two points
            PolylineOptions polylineOptions;
            for(int i = 0; i < gpsData.size(); i++)
            {
                if (i != (gpsData.size() - 1))
                {
                    //get dta for polyline
                    double val1 = list.get(i);
                    double val2 = list.get(i+1);
                    double lat1 = gpsData.get(i).getLatitude();
                    double lon1 = gpsData.get(i).getLongitude();
                    double lat2 = gpsData.get(i + 1).getLatitude();
                    double lon2 = gpsData.get(i + 1).getLongitude();
                    double numOfDiv = appData.interpolation;
                    double divDiffLat = (lat2 - lat1) / numOfDiv;
                    double divDiffLon = (lon2 - lon1) / numOfDiv;
                    double divDiffVal = (val2-val1)/numOfDiv;

                    //draw (number of interpolations) polylines
                    for (int j = 1; j <= numOfDiv; j++)
                    {
                        int red = getRedVal(min,ratio,avg,val1);
                        int green = getGreenVal(ratio, avg, val1);
                        polylineOptions = new PolylineOptions();
                        polylineOptions.add(new LatLng(lat1, lon1), new LatLng(lat1 + divDiffLat, lon1 + divDiffLon));
                        if (j < numOfDiv) polylineOptions.add(new LatLng(lat1 + divDiffLat * 2, lon1 + divDiffLon * 2));
                        else if (i!=(gpsData.size()-2)) polylineOptions.add(new LatLng(gpsData.get(i + 2).getLatitude(), gpsData.get(i + 2).getLongitude()));
                        polylineOptions.width(25);
                        polylineOptions.geodesic(true);
                        polylineOptions.color(Color.argb(100, red, green, 0));
                        //draw polyline with uiThread
                        getActivity().runOnUiThread(new PolylineTask(polylineOptions));
                        lat1 += divDiffLat;
                        lon1 += divDiffLon;
                        val1 += divDiffVal;
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(sessionData.isUseInteraction())
                {
                    //size of sphere = square(interactions)
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.strokeColor(Color.argb(80, 0, 0, 255));
                    circleOptions.fillColor(Color.argb(50, 0, 0, 255));
                    circleOptions.radius(Math.sqrt(intData.get(i)));
                    circleOptions.center(new LatLng(gpsData.get(i).getLatitude(), gpsData.get(i).getLongitude()));
                    circleOptions.zIndex(2.0f);
                    //draw sphere with uiThread
                    getActivity().runOnUiThread(new CircleTask(circleOptions));
                }
            }
        }
    }

    private class PolylineTask implements Runnable
    {
        private PolylineOptions polylineOptions;

        public PolylineTask(PolylineOptions polylineOptions)
        {
            this.polylineOptions = polylineOptions;
        }

        @Override
        public void run()
        {
            mMap.addPolyline(polylineOptions);
        }
    }

    private class CircleTask implements Runnable
    {
        private  CircleOptions circleOptions;

        public CircleTask(CircleOptions circleOptions)
        {
            this.circleOptions = circleOptions;
        }

        @Override
        public void run()
        {
            mMap.addCircle(circleOptions);
        }
    }

    //Functions for interpolation

    private int getRedVal(double min, double ratio, double avg, double val)
    {
        int red;
        if(val<avg)
        {
            red = (int)((val-min)*ratio);
        }
        else
        {
            red = 255;
        }
        return red;
    }

    private int getGreenVal(double ratio, double avg, double val)
    {
        int green;
        if(val<avg)
        {
            green = 255;
        }
        else
        {
            green = 255 - (int)((val-avg)*ratio);
        }
        return green;
    }

    private List<Double> getDoubleListByName(String name)
    {
        Log.i("MapDiagram", "get data " + name);
        List<Double> list = new ArrayList<>();
        for(int i = 0; i < gpsData.size(); i++)
        {
            switch (name)
            {
                case "Bluetooth":
                    list = bluetoothData;
                    break;
                case "Noise":
                    list = noiseData;
                    break;
                case "Speed":
                    list = speedData;
                    break;
                case "Light":
                    list = lightData;
                    break;
            }
        }
        return list;
    }

    private class ChangeListener implements AdapterView.OnItemSelectedListener
    {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            selected = usedSensorNames.get(position);
            try
            {
                executorService.shutdownNow();
            }
            catch (Exception e)
            {
                //ntd
            }
            mapReady();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    }

    @Override
    public void onResume()
    {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            executorService.shutdownNow();
        }
        catch (Exception e)
        {
            //ntd
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
