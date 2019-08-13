package at.ac.univie.jakobhirschl.contintcollector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;
import at.ac.univie.jakobhirschl.contintcollector.selection.SessionSelection;
import at.ac.univie.jakobhirschl.contintcollector.service.ContIntService;
import at.ac.univie.jakobhirschl.contintcollector.settings.SettingsActivity;

public class MainActivity extends AbstractActivity
{
    private AppData appData;
    private DBHandler dbHandler;
    private String comment = "";
    private String userStudy = "";
    private String sessionID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appData = new AppData();
        try
        {
            appData.readData(this);
        }
        catch (Exception e)
        {
            //ntd
        }
        findViewById(R.id.progress).setVisibility(View.GONE);
        Button startButton = (Button)findViewById(R.id.button);
        dbHandler = new DBHandler(this);
        if(!appData.serviceRunning) startButton.setText("Start Data Collection");
        else startButton.setText("Stop Data Collection");

    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed()
    {
        //ntd
    }

    public void startServiceButton(final View v)
    {
        LocationManager manager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        /*ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();*/
        if(appData.useGPS &&(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)))
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Please enable GPS and Network connection!");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("OK", null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else if(!appData.serviceRunning)
        {
            //textfield comment
            final EditText editText = new EditText(this);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
            sessionID = "Session_" + dateFormat.format(new Date());
            editText.setText("");
            editText.setInputType(InputType.TYPE_CLASS_TEXT);

            //textfield userStudy
            final EditText editText2 = new EditText(this);
            editText2.setText("User study");
            editText2.setInputType(InputType.TYPE_CLASS_TEXT);

            //layout
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.addView(editText);
            linearLayout.addView(editText2);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Choose comment and user study name for this session!");
            alertDialogBuilder.setView(linearLayout);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    comment = editText.getText().toString();
                    userStudy = editText2.getText().toString();
                    startServiceCommand(v);
                }
            });
            alertDialogBuilder.setNegativeButton("CANCEL", null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else startServiceCommand(v);
    }

    public void startServiceCommand(View v)
    {

        try
        {
            appData.readData(this);
        }
        catch (Exception e)
        {
            //ntd
        }
        //put extras to service
        Intent contIntService = new Intent(this,ContIntService.class);
        contIntService.putExtra(getResources().getString(R.string.IntentExtra),appData.getBooleanArray());
        contIntService.putExtra(getResources().getString(R.string.IntentExtraDB),appData.referenceAmpl);
        if(appData.interval==0) appData.interval = 1000;
        Log.i("Test",appData.interval+"");
        contIntService.putExtra(getResources().getString(R.string.IntentExtraInterval),appData.interval);
        if(!appData.serviceRunning)
        {
            //start service
            int session = dbHandler.createSession(sessionID, comment, userStudy);
            contIntService.putExtra(getResources().getString(R.string.IntentSession), session);
            startService(contIntService);
            appData.serviceRunning = true;
            appData.serialize(this);
            ((Button)v).setText("Stop Data Collection");
            Toast.makeText(this, "Start Service, Data collection will start in 10 Seconds", Toast.LENGTH_SHORT).show();
        }
        else
        {
            //stop service
            stopService(contIntService);
            appData.serviceRunning = false;
            appData.serialize(this);
            ((Button)v).setText("Start Data Collection");
            Toast.makeText(this, "Stop Service", Toast.LENGTH_SHORT).show();
        }
    }

    /*public void gotoSettings(View v)
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
    }*/

    public void gotoSessionSelection(View v)
    {
        if(appData.serviceRunning)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("You cannot visualize data while Data Collection!!");
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("OK", null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else
        {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            Intent intent = new Intent(this, SessionSelection.class);
            startActivity(intent);
        }
    }

    public void testJSON(View v)
    {
        JSONObject json = dbHandler.getJSON(1);
        JSONObject json2 = dbHandler.getJSON(2);
        List<JSONObject> list = new ArrayList<>();
        list.add(json);
        list.add(json2);
        shareMultipleJSON(list);
    }

    public void shareMultipleJSON(List<JSONObject> objects)
    {
        File directory = this.getCacheDir();
        ArrayList<Uri> uris = new ArrayList<>();
        try
        {
            //for all JSON objects
            for(int i = 0; i < objects.size(); i++)
            {
                //create temp file
                File file = new File(this.getCacheDir(), "json" + (i+1) + ".json");
                //write JSON data into file
                OutputStream os = new FileOutputStream(file.getAbsolutePath());
                os.write(objects.get(i).toString().getBytes());
                os.flush();
                os.close();
                //add uri to uri ArrayList
                uris.add(JSONContentProvider.getUriForFile(this, "at.ac.univie", file));
            }

            //start sharing action
            Intent share = new Intent();
            share.setAction(Intent.ACTION_SEND_MULTIPLE);
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            share.setType("text/plain");
            startActivity(Intent.createChooser(share, "Share"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
