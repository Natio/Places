package com.gcw.sapienza.places;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by snowblack on 1/4/15.
 */
public class JSONWeatherTask extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        String data = ( (new WeatherHttpClient()).getWeatherData(params[0]));
        int round_temp = Integer.MAX_VALUE;
        try {
            JSONObject jObj = new JSONObject(data);
            JSONObject mainObj = jObj.getJSONObject("main");

            float temp = (float)mainObj.getDouble("temp") - 273.15f;
            round_temp = Math.round(temp);
            Log.d("Weather", "Temperatura a Formia: " + round_temp);
            PlacesApplication.temperature = round_temp;

        } catch (JSONException e) {
            PlacesApplication.temperature = round_temp;
            e.printStackTrace();
        }
        Log.d("Weather", data);
        return data;
    }
}
