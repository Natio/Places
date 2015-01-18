package com.gcw.sapienza.places;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by snowblack on 1/4/15.
 *
 */
public class JSONWeatherTask extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        String data = ( (new WeatherHttpClient()).getWeatherData(params[0]));
        try {
            Log.d("Weather", data);
            JSONObject info = new JSONObject(data);
            JSONObject mainObj = info.getJSONObject("main");

            float temp = (float)mainObj.getDouble("temp") - 273.15f;
            int round_temp = Math.round(temp);
            Log.d("Weather", "Temperatura: " + round_temp);

            JSONArray weatherObj = info.getJSONArray("weather");
            String cond = weatherObj.getJSONObject(0).getString("main");
            Log.d("Weather", "Condizioni: " + cond);
            String weather = round_temp + "°C, " + cond;
            PlacesApplication.getInstance().setWeather(weather);

        } catch (JSONException | NullPointerException e) {
            PlacesApplication.getInstance().setWeather("");
            e.printStackTrace();
        }

        return data;
    }
}
