package io.github.ataias.othersunshine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    String detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //Getting data that was passed when activity opened
        String data = getIntent().getExtras().getString(Intent.EXTRA_TEXT);
        TextView textView = (TextView) findViewById(R.id.detail_text_view);
        textView.setText(data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }
}