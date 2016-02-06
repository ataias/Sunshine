package io.github.ataias.othersunshine;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    ArrayAdapter<String> mItemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] forecast = {
                "Today",  "Tomorrow", "Day after tomorrow", "Hey, it is 88 degress",
                "Hello!", "Hello world!", "E aí? Beleza?"};

        //if if you use three arguments, the method still exits but it doesn't not work
        mItemsAdapter = new ArrayAdapter<String>(
                //The current context (this fragment's activity
                this,
                //ID of the list item layout
                R.layout.list_view_item,
                //ID of the TextView
                R.id.list_item,
                forecast);

        //Container is the parent view of the fragment
        //If not used, it complains that it is a static method in a non-static environment
        //and it does not work
        ListView listView = (ListView) findViewById(R.id.main_list_view);
        listView.setAdapter(mItemsAdapter);

    }
}
