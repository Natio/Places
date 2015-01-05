package com.gcw.sapienza.places;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by snowblack on 1/4/15.
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
            Log.d("Weather", "Temperatura a Formia: " + round_temp);

            JSONArray weatherObj = info.getJSONArray("weather");
            String cond = ((JSONArray)weatherObj).getJSONObject(0).getString("main");
            Log.d("Weather", "Condizioni a Formia: " + cond);
            String weather = round_temp + "°C, " + cond;
            PlacesApplication.weather = weather;

        } catch (JSONException e) {
            PlacesApplication.weather = "";
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            PlacesApplication.weather = "";
            e.printStackTrace();
        }
        return data;
    }
}
