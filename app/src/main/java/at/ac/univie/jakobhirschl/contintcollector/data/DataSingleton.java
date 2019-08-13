package at.ac.univie.jakobhirschl.contintcollector.data;

import android.content.Context;
import android.util.Log;

import at.ac.univie.jakobhirschl.contintcollector.appdata.AppData;
import at.ac.univie.jakobhirschl.contintcollector.db.DBHandler;

/**
 * Created by Jakob Hirschl on 19.11.2015.
 *
 * Singleton for sessionData. Minimizes data loading overhead
 *
 */
public class DataSingleton
{
    private static DataSingleton instance;
    private SessionData sessionData;
    private Context context;
    private Boolean smoothed = null;

    public static DataSingleton getInstance()
    {
        if(instance==null) instance = new DataSingleton();
        return instance;
    }

    private DataSingleton() {}

    public SessionData getSessionData(int ID)
    {
        //read settings
        AppData appData = new AppData();
        try
        {
            appData.readData(context);
        }
        catch (Exception e)
        {
            //ntd
        }
        if(smoothed==null) smoothed = appData.smooth;
        DBHandler dbHandler = new DBHandler(context);
        //if session data is null, load data
        if(sessionData == null)
        {
            Log.i("Singleton","Load Session " + ID);
            sessionData = dbHandler.getDataForSession(ID,appData.smooth);
        }
        //if current ID and wanted ID is not the same -> load data
        else if(sessionData.getID()!=ID)
        {
            Log.i("Singleton","Loaded Session " + sessionData.getID() + " -> Load Session " + ID);
            sessionData = dbHandler.getDataForSession(ID,appData.smooth);
        }
        //if data should or should not be smoothed but isn't/is -> get data
        else if(smoothed!=appData.smooth)
        {
            Log.i("Singleton","Loaded Session " + sessionData.getID() + " -> Load Session " + ID);
            sessionData = dbHandler.getDataForSession(ID,appData.smooth);
        }
        smoothed = appData.smooth;
        Log.i("Singleton","Return Session " + sessionData.getID());
        //else -> just return data
        return sessionData;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }
}
