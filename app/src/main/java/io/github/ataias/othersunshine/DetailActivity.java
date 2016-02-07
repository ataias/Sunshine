package io.github.ataias.othersunshine;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    String mForecastStr;
    final String LOG_TAG = DetailActivity.class.getSimpleName();
    final String APP_HASHTAG = " #SunshineApp";
    ShareActionProvider shareAP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //Getting data that was passed when activity opened
        String data = getIntent().getExtras().getString(Intent.EXTRA_TEXT);
        TextView textView = (TextView) findViewById(R.id.detail_text_view);
        textView.setText(data);

        mForecastStr = data;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_map).setVisible(false);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        shareAP = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        shareAP.setShareIntent(getShareIntent());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsActivityIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(settingsActivityIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    private Intent getShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mForecastStr + APP_HASHTAG);

        return intent;
    }
}
