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
import android.widget.ListView;
import android.widget.Toast;

import io.github.ataias.othersunshine.data.WeatherContract;

public class ForecastActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    private static final int FORECAST_DATA_LOADER = 0;

    ForecastAdapter mForecastAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String locationSetting = Utility.getPreferredLocation(getApplicationContext());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting,
                System.currentTimeMillis()
        );

        Cursor cur = getContentResolver().query(
                weatherForLocationUri,
                null,
                null,
                null,
                sortOrder
        );

        mForecastAdapter = new ForecastAdapter(this, cur, 0);

        //Container is the parent view of the fragment
        //If not used, it complains that it is a static method in a non-static environment
        //and it does not work
        ListView listView = (ListView) findViewById(R.id.main_list_view);
        listView.setAdapter(mForecastAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(FORECAST_DATA_LOADER, null, this);

//        //classe anônima! yeye!!
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String value = (String) parent.getItemAtPosition(position);
//                Intent detailActivityIntent = new Intent(parent.getContext(), DetailActivity.class);
//                detailActivityIntent.putExtra(Intent.EXTRA_TEXT, value);
//                startActivity(detailActivityIntent);
//            }
//        });

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
        updateWeather();
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

        Uri uri = WeatherContract.WeatherEntry.CONTENT_URI.buildUpon()
                .appendPath(queryParam)
                .appendQueryParameter(WeatherContract.WeatherEntry.COLUMN_DATE, String.valueOf(currentDate))
                .build();

        String[] projection = null;
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mForecastAdapter.swapCursor(null);
    }
}
