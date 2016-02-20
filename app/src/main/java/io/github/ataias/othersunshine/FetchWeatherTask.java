package io.github.ataias.othersunshine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import io.github.ataias.othersunshine.data.WeatherContract.LocationEntry;
import io.github.ataias.othersunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by ataias on 2/6/16.
 */
//params[0] should be the zip code
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    String mQueryFormat = "json";
    String mUnits = "metric";
    int mDays = 14;
    String mId = "0614cde9f06e70606dca2278f05f6641";

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    String[] mParsedJson;

    //Preferences
    String mLocation; //can be city name (comma country) or zipcode
    boolean isMetric;

    private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;

    public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private boolean DEBUG = true;

    @Override
    protected String[] doInBackground(String... params) {

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }

        //Initialize preferences
        mLocation = params[0];
        isMetric = Integer.parseInt(params[1]) == 0;

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
                .appendQueryParameter(QUERY_PARAM, mLocation)
                .appendQueryParameter(FORMAT_PARAM, mQueryFormat)
                .appendQueryParameter(UNITS_PARAM, mUnits)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(mDays))
                .appendQueryParameter(ID_PARAM, mId);

        String urlQuery = builder.build().toString();

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
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
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
//        Log.v(LOG_TAG, forecastJsonStr);
        try {
            mParsedJson = getWeatherDataFromJson(forecastJsonStr, mLocation);
            return mParsedJson;
        } catch (final JSONException e) {
            Log.e(LOG_TAG, "Could not parse JSONString");
        }
        return null;
    }

    // MARK: JSON Parsing

    /*
    @params Tc is the temperature in celsius
     */
    private double toFahrenheit(double Tc) {
        return Tc * 9 / 5 + 32;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        if (!isMetric) {
            high = toFahrenheit(high);
            low = toFahrenheit(low);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    int queryCityName(ContentResolver resolver, String cityName) {
        Cursor cursor = resolver.query(
                LocationEntry.CONTENT_URI,
                new String[]{LocationEntry._ID, LocationEntry.COLUMN_CITY_NAME},
                LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_CITY_NAME + " = ? ",
                new String[]{cityName},
                null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(LocationEntry._ID));
            }
        } finally {
            cursor.close();
        }

        return -1;
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {

        ContentResolver resolver = mContext.getContentResolver();
        int _id = queryCityName(resolver, cityName);
        if (_id == -1) {

            //Location wasn't added before
            ContentValues value = new ContentValues();
            value.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            value.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            value.put(LocationEntry.COLUMN_COORD_LAT, lat);
            value.put(LocationEntry.COLUMN_COORD_LONG, lon);
            resolver.insert(LocationEntry.CONTENT_URI, value);

            //Should have been added by now
            _id = queryCityName(resolver, cityName);
        }

        return _id;
    }

    /*
    Students: This code will allow the FetchWeatherTask to continue to return the strings that
    the UX expects so that we can continue to test the application even once we begin using
    the database.
 */
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for (int i = 0; i < cvv.size(); i++) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
    }


    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for (int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            // add to database
            if (cVVector.size() > 0) {
                ContentResolver resolver = mContext.getContentResolver();

                resolver.bulkInsert(
                        WeatherEntry.CONTENT_URI,
                        cVVector.toArray(new ContentValues[cVVector.size()])
                );
            }

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
            Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                    locationSetting, System.currentTimeMillis());

            // Students: Uncomment the next lines to display what what you stored in the bulkInsert

//            Cursor cur = mContext.getContentResolver().query(weatherForLocationUri,
//                    null, null, null, sortOrder);
//
//            cVVector = new Vector<ContentValues>(cur.getCount());
//            if ( cur.moveToFirst() ) {
//                do {
//                    ContentValues cv = new ContentValues();
//                    DatabaseUtils.cursorRowToContentValues(cur, cv);
//                    cVVector.add(cv);
//                } while (cur.moveToNext());
//            }
//
//            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");

            String[] resultStrs = convertContentValuesToUXFormat(cVVector);

            return resultStrs;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
    * so for convenience we're breaking it out into its own method now.
    */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("E, MMM d");
        return shortenedDateFormat.format(date).toString();
    }

    @Override
    protected void onPostExecute(String[] strings) {
        super.onPostExecute(strings);

        //When an adapter is changed, it automatically calls the notifyDataSetChanged() internally
        mForecastAdapter.clear();
        mForecastAdapter.addAll(mParsedJson);
    }
}