package com.gcw.sapienza.places.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by snowblack on 1/4/15.
 */
public class WeatherHttpClient {

    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static final String IMG_URL = "http://openweathermap.org/img/w/";


    public String getWeatherData(String location) {
        HttpURLConnection con = null;
        InputStream is = null;

        try {
            con = (HttpURLConnection) (new URL(BASE_URL + location)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            // Let's read the response
            StringBuilder buffer = new StringBuilder();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
                buffer.append("\r\n");
            }
            is.close();
            con.disconnect();

            return buffer.toString();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Throwable t) {
            }
            try {
                con.disconnect();
            } catch (Throwable t) {
            }
        }

        return null;

    }

    public byte[] getImage(String code) {
        HttpURLConnection con = null;
        InputStream is = null;
        try {
            con = (HttpURLConnection) (new URL(IMG_URL + code + "&units=metric")).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            // Let's read the response
            is = con.getInputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            while (is.read(buffer) != -1)
                baos.write(buffer);

            return baos.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Throwable t) {
            }
            try {
                con.disconnect();
            } catch (Throwable t) {
            }
        }

        return null;

    }
}