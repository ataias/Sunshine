package io.github.ataias.othersunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ForecastActivity extends AppCompatActivity {

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    static ArrayAdapter<String> mItemsAdapter;

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
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateWeather();
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String city = prefs.getString(getString(R.string.pref_location_key), "Brasilia");

        String unit = prefs.getString(getString(R.string.pref_temperature_unit_key), "metric");
        new FetchWeatherTask().execute(city, unit);
    }
}
