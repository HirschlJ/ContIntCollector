package at.ac.univie.jakobhirschl.contintcollector.selection;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;

import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.vis.MapsFragment;
import at.ac.univie.jakobhirschl.contintcollector.vis.RatioDiagram;
import at.ac.univie.jakobhirschl.contintcollector.vis.SingleSensorDiagram;
import at.ac.univie.jakobhirschl.contintcollector.vis.XYDiagram;

public class TabVisSelection extends FragmentActivity
{

    private FragmentTabHost fragmentTabHost;

    private int ID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_vis_selection);

        //get session if possible, else go to Session Selection
        Intent intent = getIntent();
        ID = intent.getIntExtra(getResources().getString(R.string.IntentSession),-1);
        if(ID==-1)
        {
            Intent returnIntent = new Intent(this, SessionSelection.class);
            startActivity(returnIntent);
        }
        Log.i("TabVisSelection", "Visselection for Session " + ID);

        fragmentTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        fragmentTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        //load fragment depending on selected tab
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("All Sensors").setIndicator("All Sensors"), XYDiagram.class, XYDiagram.getBundle(ID));
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("Single Sensor").setIndicator("Single Sensor"), SingleSensorDiagram.class, SingleSensorDiagram.getBundle(ID));
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("Ratio").setIndicator("Ratio"), RatioDiagram.class, RatioDiagram.getBundle(ID));
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("Map").setIndicator("Map"), MapsFragment.class, MapsFragment.getBundle(ID));
    }
}
