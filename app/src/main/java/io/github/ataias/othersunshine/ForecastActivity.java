package io.github.ataias.othersunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ForecastActivity extends AppCompatActivity {

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    ArrayAdapter<String> mItemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mItemsAdapter = new ArrayAdapter<String>(
                //The current context (this fragment's activity
                this,
                //ID of the list item layout
                R.layout.list_view_item,
                //ID of the TextView
                R.id.list_item,
                new ArrayList<String>());

        //Container is the parent view of the fragment
        //If not used, it complains that it is a static method in a non-static environment
        //and it does not work
        ListView listView = (ListView) findViewById(R.id.main_list_view);
        listView.setAdapter(mItemsAdapter);

        //classe anônima! yeye!!
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                Intent detailActivityIntent = new Intent(parent.getContext(), DetailActivity.class);
                detailActivityIntent.putExtra(Intent.EXTRA_TEXT, value);
                startActivity(detailActivityIntent);
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

        if(id == R.id.action_map) {
            showMap();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateWeather();
    }

    private void updateWeather() {
        Resources res = getResources();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String defaultLocation = res.getString(R.string.pref_default_location);
        String city = prefs.getString(getString(R.string.pref_location_key), defaultLocation);


        String[] possibleUnits = res.getStringArray(R.array.pref_temperature_unit_values);
        String unit = prefs.getString(getString(R.string.pref_temperature_unit_key), possibleUnits[0]);

        new FetchWeatherTask(getApplicationContext(), mItemsAdapter).execute(city, unit);
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
}
