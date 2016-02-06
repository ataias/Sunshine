package io.github.ataias.othersunshine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForecastActivity extends AppCompatActivity {

    private final String LOG_TAG = ForecastActivity.class.getSimpleName();

    static ArrayAdapter<String> mItemsAdapter;
    List<String> mForecast; //list avoids problem using the clear() method of the adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Log.v(LOG_TAG, "Criou o fetchWeatherTask");

//        String[] params = {"http://api.openweathermap.org/data/2.5/forecast/daily?q=Brasilia&mode=json&units=metric&cnt=7&appid=0614cde9f06e70606dca2278f05f6641"};

        if (mForecast == null) {
            String[] fakeData = new String[]{
                    "Today", "Tomorrow", "Day after tomorrow", "Hey, it is 88 degress",
                    "Hello!", "Hello world!", "E aí? Beleza?"};

            mForecast = new ArrayList<String>(Arrays.asList(fakeData));
        }

        //if if you use three arguments, the method still exits but it doesn't not work
        mItemsAdapter = new ArrayAdapter<String>(
                //The current context (this fragment's activity
                this,
                //ID of the list item layout
                R.layout.list_view_item,
                //ID of the TextView
                R.id.list_item,
                mForecast);

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
//                Toast toast = Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT);
//                toast.show();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            new FetchWeatherTask().execute(new String[]{"94035,us"});
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
