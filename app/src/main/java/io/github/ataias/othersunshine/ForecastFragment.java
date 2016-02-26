package io.github.ataias.othersunshine;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import io.github.ataias.othersunshine.data.WeatherContract;

/**
 * Created by ataias on 2/26/16.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final long MIN_CURSOR_ENTRIES = 10;

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    private static final int FORECAST_DATA_LOADER = 0;

    ForecastAdapter mForecastAdapter;

    public ForecastFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.main_list_view);
        listView.setAdapter(mForecastAdapter);

        //classe anônima! yeye!!
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Intent intent = new Intent(getActivity(), DetailActivity.class);
                    intent.setData(
                            WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting,
                                    cursor.getLong(ForecastAdapter.COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_DATA_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        long currentDate = Utility.getCurrentTime();
        String queryParam = Utility.getPreferredLocation(getActivity());

        //This line gives the link to the content provider and query parameters
        Uri uri = WeatherContract.WeatherEntry.CONTENT_URI.buildUpon()
                .appendPath(queryParam)
                .appendQueryParameter(WeatherContract.WeatherEntry.COLUMN_DATE, String.valueOf(currentDate))
                .build();

        String[] projection = ForecastAdapter.FORECAST_COLUMNS;
        String selection = null;
        String[] selectionArgs = null;

        return new CursorLoader(
                getActivity(),
                uri,
                projection,
                selection,
                selectionArgs,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mForecastAdapter.swapCursor(null);
    }

    private void updateWeather() {
        String city = Utility.getPreferredLocation(getActivity());
        String unit = getCurrentTemperatureUnit();

        new FetchWeatherTask(getActivity()).execute(city, unit);
    }

    private String getCurrentTemperatureUnit() {
        Resources res = getResources();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String[] possibleUnits = res.getStringArray(R.array.pref_temperature_unit_values);
        String unit = prefs.getString(getString(R.string.pref_temperature_unit_key), possibleUnits[0]);
        return unit;
    }

    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_DATA_LOADER, null, this);
    }
}
