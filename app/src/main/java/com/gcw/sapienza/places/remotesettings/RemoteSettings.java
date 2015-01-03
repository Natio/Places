package com.gcw.sapienza.places.remotesettings;

import android.content.Context;
import android.util.Log;

import com.gcw.sapienza.places.PlacesApplication;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;




/**
 * Class for configuring parameters of the app from a remote configuration file
 *
 *
 *
 * TUTORIAL
 *
 *  This class has a single instance that can be retrieved using:

        RemoteSettings.getInstance();

 *   To synchronize with the file on the server call this method when the app launches
 *
 *
        RemoteSettings.getInstance().synchWithFileAtURL("http://someURL", callbacks);

 *   It will download a JSON file containing the configurations. If it cannot retrieve the JSON file from the specified URL it will use a
 *   previously saved configuration file located in the internal storage.
 *
 *  To retrieve a value from the settings use the following method:

    RemoteSettings.getInstance().getValue(key, defaultVal);
 *
 *  defaultVal must be of the same type of the expected value, otherwise a ClassCastException is raised
 *
 *
 */
public class RemoteSettings {
    private static final String TAG = "RemoteSettings";
    //name of local copy of remote file
    private static final String CONFIGURATION_FILE_NAME = "remote_config.json";


    private JSONObject storage = null;

    /**
     * Singleton stuff :) see here for reference http://it.wikipedia.org/wiki/Singleton#Esempio_di_inizializzazione_lazy:_Java
     */
    private static class Container{
        private final static RemoteSettings ISTANCE = new RemoteSettings();
    }

    /**
     * Returns a reference to the RemoteSettings singleton
     * This is the only way or obtaining a RemoteSettings object
     * @return the shared instance
     */
    public static RemoteSettings getInstance() {
        return Container.ISTANCE;
    }

    /**
     * Private constructor, in this way it is not possible to directly instanciate the object
     */
    private RemoteSettings(){
        this.storage = this.readConfigurationFromFile(CONFIGURATION_FILE_NAME);
    }

    private JSONObject readConfigurationFromFile(String fileName){
        try {
            FileInputStream inputStream = PlacesApplication.getPlacesAppContext().openFileInput(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            br.close();
            Log.d(TAG, "Configuration read from file");
            return new JSONObject(sb.toString());

        }
        catch (FileNotFoundException e) {
            Log.d(TAG,"No file found "+e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (JSONException e){
            Log.d(TAG,"Error parsing JSON "+e.getMessage());
            e.printStackTrace();
        }

        return new JSONObject();
    }

    private void writeConfigurationToFile(String fileName, JSONObject configuration){
        Context ctx = PlacesApplication.getPlacesAppContext();
        try {
            FileOutputStream outputStream = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(configuration.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads and configures the object with the a resource at the given URL
     * This is an asynchronous method, therefore calling getValue before it completes will return old values
     * @param url configuration file's URL
     */
    public void synchWithFileAtURL(String url, final RemoteSettingsCallBacks cbks){

        //Create an asynchronous HTTP request
        //if the request succeeds the storage is overwritten with the new data otherwise a description is printed on console
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                RemoteSettings.this.writeConfigurationToFile(CONFIGURATION_FILE_NAME, response);
                RemoteSettings.this.storage = response;
                Log.d(TAG, "RemoteSettings: successfully downloaded config from " + this.getRequestURI().toString());
                if(cbks != null){
                    cbks.onRemoteConfig();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray time_line) {
                if(cbks != null){
                    cbks.onError("Invalid json settings format. Json object expected, json array received");
                }
            }

            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if(cbks != null){
                    cbks.onError("http code"+ statusCode+ ". "+ (throwable == null ? "" : throwable.toString()) );
                }
            }
        });
    }




    /**
     *
     * @param key the key of the value
     * @param defaultValue default value to return
     * @param <T> type of the value
     * @return returns the settings value if present, otherwise will return defaultValue
     */
    @SuppressWarnings({"unchecked","unused"})
    public <T> T getValue(String key, T defaultValue){
        if(this.storage.has(key)){
            try{
                //obviously it can raise a class cast exception, but this is the correct behaviour. We want coherency between the expected value and the default value
                return (T)this.storage.get(key);
            }
            catch (JSONException e){
                Log.e(TAG, e.getMessage());
                return defaultValue;
            }

        }
        return defaultValue;
    }
}
