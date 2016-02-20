package io.github.ataias.othersunshine;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import io.github.ataias.othersunshine.data.WeatherContract;

public class ForecastActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final long MIN_CURSOR_ENTRIES = 10;

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    private static final int FORECAST_DATA_LOADER = 0;

    ForecastAdapter mForecastAdapter;
    String mLocation; //last known location

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mForecastAdapter = new ForecastAdapter(this, null, 0);

        ListView listView = (ListView) findViewById(R.id.main_list_view);
        listView.setAdapter(mForecastAdapter);

        getLoaderManager().initLoader(FORECAST_DATA_LOADER, null, this);

        //classe anônima! yeye!!
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getApplicationContext());
                    Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                    intent.setData(
                            WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting,
                                    cursor.getLong(ForecastAdapter.COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_share).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent settingsActivityIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(settingsActivityIntent);
            return true;
        }

        if (id == R.id.action_map) {
            showMap();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //If user changes settings and then click "back" button,
        // this guarantees that new location settings will be loaded
        if (mLocation != null)
            if (!mLocation.equals(Utility.getPreferredLocation(getApplicationContext()))) {
                getLoaderManager().restartLoader(FORECAST_DATA_LOADER, null, this);
                mLocation = Utility.getPreferredLocation(getApplicationContext());
            }
    }

    private String getCurrentTemperatureUnit() {
        Resources res = getResources();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String[] possibleUnits = res.getStringArray(R.array.pref_temperature_unit_values);
        String unit = prefs.getString(getString(R.string.pref_temperature_unit_key), possibleUnits[0]);
        return unit;
    }

    private void updateWeather() {
        String city = Utility.getPreferredLocation(getApplicationContext());
        String unit = getCurrentTemperatureUnit();

        new FetchWeatherTask(getApplicationContext()).execute(city, unit);
    }

    private void showMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_default_location));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + location));
        boolean mapAppFound = intent.resolveActivity(getPackageManager()) != null;
        if (mapAppFound) {
            startActivity(intent);
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Map application not found";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }


    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        long currentDate = Utility.getCurrentTime();
        String queryParam = Utility.getPreferredLocation(getApplicationContext());

        //This line gives the link to the content provider and query parameters
        Uri uri = WeatherContract.WeatherEntry.CONTENT_URI.buildUpon()
                .appendPath(queryParam)
                .appendQueryParameter(WeatherContract.WeatherEntry.COLUMN_DATE, String.valueOf(currentDate))
                .build();

        String[] projection = ForecastAdapter.FORECAST_COLUMNS;
        String selection = null;
        String[] selectionArgs = null;

        return new CursorLoader(
                this,
                uri,
                projection,
                selection,
                selectionArgs,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //The loader will release the data once it knows the application
        // is no longer using it. For example, if the data is a cursor
        // from a CursorLoader, you should not call close() on it yourself.
        mForecastAdapter.swapCursor(data);

        //Only get weather data from internet if number of cursor entries is smaller
        // then MIN_CURSOR_ENTRIES
        if (data.getCount() < MIN_CURSOR_ENTRIES) {
            updateWeather();
        }

        mLocation = Utility.getPreferredLocation(getApplicationContext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mForecastAdapter.swapCursor(null);
    }
}
