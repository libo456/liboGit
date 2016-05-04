package com.example.sitesas.clima;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.example.sitesas.clima.data.WeatherContract;
import com.example.sitesas.clima.data.WeatherDbHelper;

import java.util.HashSet;

/**
 * Created by jnavia on 4/28/16.
 */
public class TestDB extends AndroidTestCase {
    public static final String LOG_TAG = TestDB.class.getSimpleName();

    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void setUp(){
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);

        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals("xxxx",true, db.isOpen());

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        assertTrue("Error: This means that the database has not been created correctly", c.moveToFirst());

        // Verify that the tables have been created
        do{
            tableNameHashSet.remove(c.getString(0));
        }while(c.moveToNext());

        // if this fails, it means that your database doesn't contain both the location entry
        assertTrue("Error: Your database was created without both the location entry " +
                "and weather entry tables", tableNameHashSet.isEmpty());

        c= db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME+ ")", null);

        assertTrue("Error: This means that we were unable to query the database for the table's information.",
                c.moveToFirst());

        // Build HashSet of all of the culumn names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);

        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columNameIndex = c.getColumnIndex("name");
        do{
            String columnName = c.getString(columNameIndex);
            locationColumnHashSet.remove(columnName);
        }while(c.moveToNext());

        // If this fails, it means that your database does not contain all of the required location.
        assertTrue("Error: The database does not contain all of the required location "+
                "entry columns.", locationColumnHashSet.isEmpty());

        c.close();
        db.close();
    }
}
