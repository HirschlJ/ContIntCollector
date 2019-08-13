package at.ac.univie.jakobhirschl.contintcollector;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by jakobhirschl on 10.03.16.
 */
public class JSONContentProvider extends FileProvider
{

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Log.i("Hier","query");
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        /*
        Log.i("JSONContextProvider","uri: " + uri.toString());
        String string = uri.toString();
        string = (string.split("/"))[string.split("/").length-1];
        Log.i("JSONContextProvider","Read file: " + string);
        File privateFile = new File(getContext().getCacheDir(), string);
        return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY);
        */
        //Log.i("Hier","open");
        return super.openFile(uri,mode);
    }
}
