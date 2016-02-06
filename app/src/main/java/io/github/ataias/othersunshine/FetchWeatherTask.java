package io.github.ataias.othersunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ataias on 2/6/16.
 */
//params[0] should be the zip code
public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    String mQueryFormat = "json";
    String mUnits = "metric";
    String mDays = "7";
    String mId = "0614cde9f06e70606dca2278f05f6641";

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    @Override
    protected Void doInBackground(String... params) {
        //Forecast request
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String ID_PARAM = "appid";

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("api.openweathermap.org").appendPath("data").appendPath("2.5")
                .appendPath("forecast").appendPath("daily")
                .appendQueryParameter(QUERY_PARAM, params[0])
                .appendQueryParameter(FORMAT_PARAM, mQueryFormat)
                .appendQueryParameter(UNITS_PARAM, mUnits)
                .appendQueryParameter(DAYS_PARAM, mDays)
                .appendQueryParameter(ID_PARAM, mId);

        String urlQuery = builder.build().toString();
        Log.v(LOG_TAG, "URI Bult is " + urlQuery);

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
//                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=Brasilia&mode=json&units=metric&cnt=7&appid=0614cde9f06e70606dca2278f05f6641");
            URL url = new URL(urlQuery);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(this.LOG_TAG, "Error closing stream", e);
                }
            }
        }
        Log.v(LOG_TAG, forecastJsonStr);
        return null;
    }
}