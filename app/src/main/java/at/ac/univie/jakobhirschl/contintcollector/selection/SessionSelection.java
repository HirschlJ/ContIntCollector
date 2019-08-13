package at.ac.univie.jakobhirschl.contintcollector.selection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import at.ac.univie.jakobhirschl.contintcollector.AbstractActivity;
import at.ac.univie.jakobhirschl.contintcollector.JSONContentProvider;
import at.ac.univie.jakobhirschl.contintcollector.R;
import at.ac.univie.jakobhirschl.contintcollector.data.DataSingleton;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;

public class SessionSelection extends AbstractActivity
{
    ListView listView;
    List<Integer> idList;
    String[] items;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //get dbhandler
        DBHandler dbHandler = new DBHandler(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_selection);
        findViewById(R.id.progress).setVisibility(View.GONE);
        listView = (ListView)findViewById(R.id.session_list);

        //get all sessions -> put session number in a tree to sort them
        List<String> listValues = new ArrayList<>();
        Map<Integer,String> list = dbHandler.getSessions();
        SortedSet<Integer> sortedKeys = new TreeSet<Integer>(list.keySet());
        idList = new ArrayList<>();
        //put the sorted sessions in a list
        for(Integer key : sortedKeys)
        {
            listValues.add(list.get(key) + " (Session " + key + ")");
            idList.add(key);
        }
        //assign listValues to String array
        items = listValues.toArray(new String[listValues.size()]);
        //assign list to the listView
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1,listValues.toArray(new String[listValues.size()]));
        listView.setAdapter(adapter);
        //set listener
        SessionClickListener sessionClickListener = new SessionClickListener(this);
        listView.setOnItemClickListener(sessionClickListener);
        //listView.setOnItemLongClickListener(sessionClickListener);
        registerForContextMenu(listView);

    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(items[info.position]);
        menu.add(0, 0, 0, "Share");
        menu.add(0,1,1,"Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        if(menuItem.getItemId()==1)
        {
            delete(info.position);
        }
        else
        {
            JSONObject json = (new DBHandler(this)).getJSON(idList.get(info.position));
            shareSingleJSON(json, idList.get(info.position));
        }
        return true;
    }

    public boolean delete(final int position)
    {
        Log.i("Test", items[position] + " " + idList.get(position));
        final DBHandler dbHandler = new DBHandler(this);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Do you want to delete \"" + items[position] +  "\" and all data stored with it?");
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton("YES!", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dbHandler.deleteSession(idList.get(position));
                finish();
                startActivity(getIntent());
            }
        });
        alertDialogBuilder.setNegativeButton("NO!", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        return true;
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        //hide progress bar
        findViewById(R.id.progress).setVisibility(View.GONE);
    }


    private class SessionClickListener implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
    {
        private Context context;

        public SessionClickListener(Context context)
        {
            this.context = context;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            //on click -> load data and goto TabVisSelection
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            DataSingleton dataSingleton = DataSingleton.getInstance();
            dataSingleton.setContext(context);
            dataSingleton.getSessionData(idList.get(position));
            Intent intent =  new Intent(context, TabVisSelection.class);
            intent.putExtra(getResources().getString(R.string.IntentSession),idList.get(position));
            startActivity(intent);
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
        {
            //on long click -> show alert -> delete session if confirmed
            return true;
        }
    }

    //Share a single JSON Object
    public void shareSingleJSON(JSONObject json, int id)
    {
        File directory = this.getCacheDir();
        try
        {
            //create file in cache
            File file = new File(this.getCacheDir(), json.get("_id") + ".json");
            //write JSON data into file
            OutputStream os = new FileOutputStream(file.getAbsolutePath());
            os.write(json.toString().getBytes());
            os.flush();
            os.close();

            //get uri for file
            Uri uri = JSONContentProvider.getUriForFile(this, "at.ac.univie", file);

            //start sharing action
            Intent share = new Intent();
            share.setAction(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setType("text/plain");
            startActivity(Intent.createChooser(share, "Share"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void shareMultipleJSON(List<JSONObject> objects, List<Integer> ids)
    {
        ArrayList<Uri> uris = new ArrayList<>();
        try
        {
            //for all JSON objects
            for(int i = 0; i < objects.size(); i++)
            {
                //create file
                File file = new File(this.getCacheDir(), objects.get(i).get("_id") + ".json");
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
