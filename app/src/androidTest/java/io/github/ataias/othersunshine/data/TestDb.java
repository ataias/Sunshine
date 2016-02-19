package io.github.ataias.othersunshine.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;

/**
 * Created by ataias on 2/11/16.
 */
public class TestDb extends AndroidTestCase {
    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /*
       This function gets called before each test is executed to delete the database.  This makes
       sure that we always have a clean test.
    */
    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {

        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);

        //Get reference to a writable database
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();

        //check that it is open
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
    Students:  Here is where you will build code to test that we can insert and query the
    location database.  We've done a lot of work for you.  You'll want to look in TestUtilities
    where you can uncomment out the "createNorthPoleLocationValues" function.  You can
    also make use of the ValidateCurrentRecord function from within TestUtilities.
*/
    public void testLocationTable() throws Throwable {
        insertLocation();
    }

    /*
     Students:  Here is where you will build code to test that we can insert and query the
     database.  We've done a lot of work for you.  You'll want to look in TestUtilities
     where you can use the "createWeatherValues" function.  You can
     also make use of the validateCurrentRecord function from within TestUtilities.
  */
    public void testWeatherTable() {
    // First insert the location, and then use the locationRowId to insert
    // the weather. Make sure to cover as many failure cases as you can.
        long locationRowId = insertLocation();

    // Instead of rewriting all of the code we've already written in testLocationTable
    // we can move this code to insertLocation and then call insertLocation from both
    // tests. Why move it? We need the code to return the ID of the inserted location
    // and our testLocationTable can only return void because it's a test.

    // First step: Get reference to writable database
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();

    // Create ContentValues of what you want to insert
    // (you can use the createWeatherValues TestUtilities function if you wish)
        ContentValues weatherRow = TestUtilities.createWeatherValues(locationRowId);

    // Insert ContentValues into database and get a row ID back
        long rowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherRow);
        assertNotSame(rowId, -1);

    // Query the database and receive a Cursor back
        Cursor queryResult = db.rawQuery("SELECT * FROM " + WeatherContract.WeatherEntry.TABLE_NAME, null);

    // Move the cursor to a valid database row
        boolean isNotEmpty = queryResult.moveToFirst();

    // Validate data in resulting Cursor with the original ContentValues
    // (you can use the validateCurrentRecord function in TestUtilities to validate the
    // query if you like)
        TestUtilities.validateCurrentRecord("Value read is different from value inserted.", queryResult, weatherRow);

    // Finally, close the cursor and database
        queryResult.close();
        db.close();
    }

    /*
        Students: This is a helper method for the testWeatherTable quiz. You can move your
        code from testLocationTable to here so that you can call this code from both
        testWeatherTable and testLocationTable.
     */
    public long insertLocation() {
        // First step: Get reference to writable database
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();

        // Create ContentValues of what you want to insert
        ContentValues locationRow = TestUtilities.createNorthPoleLocationValues();

        // Insert ContentValues into database and get a row ID back
        long rowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, locationRow);

        //-1 indicates there was an error, as the value returned is a positive rowId
        assertNotSame(rowId, -1);

        // Query the database and receive a Cursor back
        Cursor queryResult = db.rawQuery("SELECT * FROM " + WeatherContract.LocationEntry.TABLE_NAME, null);

        // Move the cursor to a valid database row
        boolean isNotEmpty = queryResult.moveToFirst();

        // Validate data in resulting Cursor with the original ContentValues
        String columnName;

        //Assert location setting
        columnName = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING;
        assertEquals("Column \"" + columnName + "\"" + " not inserted correctly: ",
                locationRow.get(columnName),
                queryResult.getString(queryResult.getColumnIndex(columnName)));

        //Assert city name
        columnName = WeatherContract.LocationEntry.COLUMN_CITY_NAME;
        assertEquals("Column \"" + columnName + "\"" + " not inserted correctly: ",
                locationRow.get(columnName),
                queryResult.getString(queryResult.getColumnIndex(columnName)));

        //Assert latitude
        columnName = WeatherContract.LocationEntry.COLUMN_COORD_LAT;
        assertEquals("Column \"" + columnName + "\"" + " not inserted correctly: ",
                locationRow.get(columnName),
                queryResult.getDouble(queryResult.getColumnIndex(columnName)));

        //Assert longitude
        columnName = WeatherContract.LocationEntry.COLUMN_COORD_LONG;
        assertEquals("Column \"" + columnName + "\"" + " not inserted correctly: ",
                locationRow.get(columnName),
                queryResult.getDouble(queryResult.getColumnIndex(columnName)));

        //Guarantee there is only one record
        assertFalse("Error, more than one record returned from location query.", queryResult.moveToNext());

        // Finally, close the cursor and database
        queryResult.close();
        db.close();

        return rowId;
    }
}
