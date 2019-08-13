package at.ac.univie.jakobhirschl.contintcollector;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;
import at.ac.univie.jakobhirschl.contintcollector.settings.SettingsActivity;

//Abstract activity for uniform design
public class AbstractActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abstract);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu)
    {
        AppData appData = new AppData();
        try
        {
            appData.readData(this);
        }
        catch (Exception e)
        {
            //ntd
        }
        super.onOptionsItemSelected(menu);
        if(menu.getItemId()==R.id.settings && !this.getClass().toString().equals(SettingsActivity.class.toString()))
        {
            if(appData.serviceRunning)
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("You cannot change the settings while Data Collection!");
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setPositiveButton("OK", null);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            else
            {
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            }
        }
        else if(menu.getItemId()==R.id.home && !this.getClass().toString().equals(MainActivity.class.toString()))
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        return true;
    }
}
