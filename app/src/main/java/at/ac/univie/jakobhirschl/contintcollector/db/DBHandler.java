package at.ac.univie.jakobhirschl.contintcollector.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import at.ac.univie.jakobhirschl.contintcollector.data.SessionData;

/**
 * Created by Jakobus on 13.11.2015.
 */
public class DBHandler extends SQLiteOpenHelper implements DBInterface
{
    private final static String dbName = "ContInt.db";
    private final static int version = 1;
    private Context context;


    public DBHandler(Context context)
    {
        super(context, dbName, null, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //Execute sql commands definded in DBInterface
        db.execSQL(CREATE_TABLE_SESSION);
        db.execSQL(CREATE_TABLE_DATA);
        Log.d("DBHandler", "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //delete old tables and create new ones
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
        Log.d("DBHandler", "Database upgrade");
        onCreate(db);
    }

    //create a new Session db entry
    public int createSession(String sessionID, String commentname, String userStudy)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SESSION_COL2,sessionID);
        values.put(SESSION_COL3,commentname);
        values.put(SESSION_COL4,userStudy);
        long longID = db.insert(TABLE_SESSION, null, values);
        int ID = (int)longID;
        Log.d("dbInsert", "Added sessionID " + sessionID + " ID " + ID);
        db.close();
        return ID;
    }

    //create new Data tupple
    public void addData(int ID, long timestamp, String data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DATA_COL1, ID);
        values.put(DATA_COL2, timestamp);
        values.put(DATA_COL3, data);
        db.insert(TABLE_DATA, null, values);
        Log.d("dbInsert", "Added data: to " + ID);
        db.close();
    }

    //get All Sessions
    public HashMap<Integer,String> getSessions()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SESSION, null);
        HashMap<Integer,String> sessions = new HashMap<>();
        if(cursor!=null)
        {
            if(cursor.getCount()>0)
            {
                cursor.moveToFirst();
                while(!cursor.isAfterLast())
                {
                    int ID = cursor.getInt(cursor.getColumnIndex(SESSION_COL1));
                    String sessionID = cursor.getString(cursor.getColumnIndex(SESSION_COL2));
                    sessions.put(ID,sessionID);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        db.close();
        Log.d("dbGet", "Get all sessions");
        return sessions;
    }

    //get data for a specific session
    public SessionData getDataForSession(int ID, boolean smooth)
    {

        SQLiteDatabase db = getReadableDatabase();
        String [] argument = new String[1];
        argument[0] = String.valueOf(ID);
        Cursor cursor = db.rawQuery(GET_DATA,argument);
        List <JSONObject> jsonObjects = new ArrayList<>();
        if(cursor!=null)
        {
            if(cursor.getCount()>0)
            {
                cursor.moveToFirst();
                while (!cursor.isAfterLast())
                {
                    try
                    {
                        jsonObjects.add(new JSONObject(cursor.getString(cursor.getColumnIndex(DATA_COL3))));
                    }
                    catch(Exception e)
                    {
                        Log.i("DBJSON","Something went terribly wrong!");
                    }

                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        db.close();
        Log.d("dbGet", "Get data for " + ID + " Length : " + jsonObjects.size());
        try
        {
            return new SessionData(ID,jsonObjects, smooth);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //delete a session
    public void deleteSession(int ID)
    {
        SQLiteDatabase db = getWritableDatabase();
        boolean deleted = false;
        if(db.delete(TABLE_DATA, DATA_COL1 + " = " + String.valueOf(ID), null)>0 && db.delete(TABLE_SESSION, SESSION_COL1 + " = " + String.valueOf(ID),null)>0) deleted = true;
        if(deleted) Log.d("dbDelete", "deleted session " + ID);
        else if(db.delete(TABLE_SESSION, SESSION_COL1 + " = " + String.valueOf(ID),null)>0) deleted = true;
        if(deleted) Log.d("dbDelete", "deleted session " + ID);
        else Log.d("dbDelete", "failed to delete session " + ID);
    }

    //reset the database -> deletes all data
    public void reset()
    {
        Log.d("dbReset", "DB Reset");
        SQLiteDatabase db = getWritableDatabase();
        onUpgrade(db, version, version);
    }

    public JSONObject getJSON(int ID)
    {
        SQLiteDatabase db = getReadableDatabase();
        String [] argument = new String[1];
        argument[0] = String.valueOf(ID);

        //create JSON Object
        JSONObject jsonObject = new JSONObject();

        //get Metadata
        String id = "";
        String deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceType = Build.MODEL;
        String sessionID = "";
        String sessionComment = "";
        String userStudy = "";
        String androidVersion = "" + Build.VERSION.SDK_INT;

        //request session information
        Cursor cursor = db.rawQuery(GET_SESSION,argument);
        if(cursor!=null)
        {
            cursor.moveToFirst();
            sessionID = cursor.getString(cursor.getColumnIndex(SESSION_COL2));
            sessionComment = cursor.getString(cursor.getColumnIndex(SESSION_COL3));
            userStudy = cursor.getString(cursor.getColumnIndex(SESSION_COL4));
        }
        cursor.close();

        id = sessionID + "_" + deviceID;

        //put data into JSON
        try
        {
            jsonObject.put("_id", id);
            jsonObject.put("deviceID", deviceID);
            jsonObject.put("deviceType", deviceType);
            jsonObject.put("sessionID", sessionID);
            jsonObject.put("sessionComment", sessionComment);
            jsonObject.put("userStudy", userStudy);
            jsonObject.put("androidVersion", androidVersion);
        }
        catch(Exception e)
        {
            //ntd
        }

        //put all data into JSON array
        cursor = db.rawQuery(GET_DATA,argument);
        JSONArray jsonArray = new JSONArray();
        if(cursor!=null)
        {
            if(cursor.getCount()>0)
            {
                cursor.moveToFirst();
                while (!cursor.isAfterLast())
                {
                    try
                    {
                        jsonArray.put(new JSONObject(cursor.getString(cursor.getColumnIndex(DATA_COL3))));
                    }
                    catch(Exception e)
                    {
                        Log.i("DBJSON","Something went terribly wrong!");
                    }

                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        db.close();
        try
        {
            jsonObject.put("data",jsonArray);
        }
        catch (Exception e)
        {
            //ntd
        }
        Log.d("dbGet", "Get JSON object for " + ID);
        return jsonObject;
    }

}
