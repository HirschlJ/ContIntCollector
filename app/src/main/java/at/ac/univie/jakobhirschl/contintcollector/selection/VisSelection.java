package at.ac.univie.jakobhirschl.contintcollector.selection;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.R;

//Only for testing
public class VisSelection extends AppCompatActivity
{
    int ID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vis_selection);
        findViewById(R.id.progress3).setVisibility(View.GONE);
        Intent intent = getIntent();
        ID = intent.getIntExtra(getResources().getString(R.string.IntentSession),-1);
        if(ID==-1)
        {
            Intent returnIntent = new Intent(this, SessionSelection.class);
            startActivity(returnIntent);
        }
        Log.i("VisSelection","Visselection for Session " + ID);
        ListView listView = (ListView)findViewById(R.id.vis_list);

        List<String> listValues = new ArrayList<>();

        listValues.add("XY-Diagram");
        listValues.add("Single-Sensor-Diagram");
        listValues.add("Ratio-Diagram");
        listValues.add("Map");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1,listValues.toArray(new String[listValues.size()]));


        listView.setAdapter(adapter);

        SessionClickListener sessionClickListener = new SessionClickListener(this);
        listView.setOnItemClickListener(sessionClickListener);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        findViewById(R.id.progress3).setVisibility(View.GONE);
    }

    private class SessionClickListener implements AdapterView.OnItemClickListener
    {
        private Context context;

        SessionClickListener(Context context)
        {
            this.context = context;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            /*
            findViewById(R.id.progress3).setVisibility(View.VISIBLE);
            //Intent depending on vis tech
            Intent intent;
            if(position==0)
            {
                intent = new Intent(context, XYDiagram.class);
            }
            else if(position==1)
            {
                intent = new Intent(context, SingleSensorDiagram.class);
            }
            else if(position==2)
            {
                intent = new Intent(context, RatioDiagram.class);
            }
            else if(position==3)
            {
                intent = new Intent(context, MapsFragment.class);
            }
            else return;
            intent.putExtra(getResources().getString(R.string.IntentSession),ID);
            startActivity(intent);
            */
        }
    }
}
