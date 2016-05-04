package com.example.sitesas.clima;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by jnavia on 5/4/16.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;

    public FetchWeatherTask(Context context){
        mContext = context;
    }

    private boolean DEBUG = true;

    private String formatHighLows(double high, double low, String unitType) {

        if(unitType.equals(getString(R.string.pref_units_imperial))){
            high = (high*1.8) + 32;
            low = (low*1.8) + 32;
        }else if(!unitType.equals(getString(R.string.pref_units_metric))){
            Log.d(LOG_TAG, "Unit type not found");
        }

        double highRounded = Math.round(high);
        double lowRounded = Math.round(low);
        return highRounded + "/" + lowRounded;
    }

    @Override
    protected String[] doInBackground(String... params){
        // if ther's no zip code, there's nothing to look up. Verify size of params
        if(params.length == 0){
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String format = "json";
        String units = "metric";
        int numDays = 7;

        // Will contain the raw JSON response as a string
        String forecastJsonStr = null;
        try{
            // http://opneweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.v(LOG_TAG, "Built URI " + builtUri.toString());

            // Create the request to open OpenWeatherMap, and open the connexion.
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if(inputStream == null){
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null){
                buffer.append(line + "\n");
            }

            if(buffer.length() == 0){
                // Stream was empty. No point is paring
                return null;
            }
            forecastJsonStr = buffer.toString();

        }catch (IOException e){
            Log.e("PlaceholderFragment", "Error ", e);
            return null;
        }finally{
            if(reader != null){
                try {
                    reader.close();
                }catch(final IOException e){
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }

        try{
            return getWeatherDataFromJson(forecastJsonStr, numDays);
        }catch(JSONException e){
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;

    }

    @Override
    protected  void onPostExecute(String[] result){
        if(result != null){
            mForecastAdapter.clear();
            for(String dayForecastStr : result){
                mForecastAdapter.add(dayForecastStr);
            }
        }
    }

    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException{
        // These are the names of the JSON objects that need to be extracted
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        Calendar gc = new GregorianCalendar();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String unitType = sharedPrefs.getString( getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric));

        String[] resultStrs = new String[numDays];

        for(int i = 0; i < weatherArray.length(); i++){
            String day;
            String description;
            String highAndLow;

            JSONObject dayForecast = weatherArray.getJSONObject(i);
            long dateTime;

            day = gc.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
            gc.add(Calendar.DAY_OF_WEEK, 1);

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low, unitType);
            resultStrs[i] = day + " -  " + description + " - " + highAndLow;
            Log.v("JsonObject", resultStrs[i]);
        }

        return resultStrs;
    }
}
