package com.gcw.sapienza.places.utils;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.models.PlacesUser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by mic_head on 27/02/15.
 */
public class GPlusUtils {

    private static final String TAG = "GPlusUtils";

    private static final GPlusUtils shared_instance = new GPlusUtils();
    private static final String API_KEY = "AIzaSyCCWHhHY20hJqIzjMQfGnwfYDv1wU_W1GU";
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private Person currentPerson;
    private boolean mIntentInProgress;

    private GPlusUtils() {
    }

    /**
     * This is a singleton class. This method returns the ONLY instance
     *
     * @return Singleton instance
     */
    public static GPlusUtils getInstance() {
        return GPlusUtils.shared_instance;
    }

    public static void getGPlusUsername(Activity activity) {
        Plus.PeopleApi.loadVisible(GPlusUtils.getInstance().getGoogleApiClient(), null).setResultCallback((ResultCallback<People.LoadPeopleResult>) activity);

        if (Plus.PeopleApi.getCurrentPerson(GPlusUtils.getInstance().getGoogleApiClient()) != null) {
            GPlusUtils.getInstance().setCurrentPerson(Plus.PeopleApi.getCurrentPerson(GPlusUtils.getInstance().getGoogleApiClient()));
            String personName = GPlusUtils.getInstance().getCurrentPerson().getDisplayName();
            String personId = GPlusUtils.getInstance().getCurrentPerson().getId();

            PlacesLoginUtils.getInstance().setCurrentUserId(personId);
            PlacesLoginUtils.getInstance().addEntryToUserIdMap(personId, personName);
        }
    }

    public static void getGPlusFriends(Activity activity) {
        if (GPlusUtils.getInstance().getGoogleApiClient() != null && GPlusUtils.getInstance().getCurrentPerson() != null)
            Plus.PeopleApi.loadVisible(GPlusUtils.getInstance().getGoogleApiClient(), GPlusUtils.getInstance().getCurrentPerson().getId()).setResultCallback((ResultCallback<People.LoadPeopleResult>) activity);
        else {
            Toast.makeText(activity, "Cannot retrieve G+ info, please login again.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "G+ friends cannot be retrieved.");
        }
    }

    public void downloadGPlusInfo(Activity activity) {
        setGoogleApiClient(new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) activity)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) activity)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build());

        getGoogleApiClient().connect();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
    }

    public Person getCurrentPerson() {
        return currentPerson;
    }

    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
    }

    @Deprecated
    public String getGPlusUsername() {
        return ((PlacesUser) ParseUser.getCurrentUser()).getName();
    }


    public void getGPlusProfilePictureURL(final String user_id, final PlacesLoginUtils.PicSize size, ImageView iv, PlacesUtilCallback cbk) {
        // FIXME size parameter is not being used
        String urlToRead = "https://www.googleapis.com/plus/v1/people/" + user_id + "?fields=image&key=" + API_KEY;

        Log.d(TAG, "Request URL: " + urlToRead);

        GetPicTask gpt = new GetPicTask(user_id, iv, cbk);
        gpt.execute(urlToRead);
    }

    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size, PlacesUtilCallback cbk) {
        getGPlusProfilePictureURL(user_id, size, imageView, cbk);
    }

    // It should never be executed + it has not been tested
    protected void loadUsernameIntoTextView(String userId, final TextView tv) {
        GetUsernameTask gut = new GetUsernameTask(userId, tv);
        String urlToRead = "https://www.googleapis.com/plus/v1/people/" + userId + "?fields=displayName&key=" + API_KEY;
        gut.execute(urlToRead);
    }

    class GetPicTask extends AsyncTask<String, String, String> {
        private String userId;
        private ImageView imageView;
        private PlacesUtilCallback cbk;

        public GetPicTask(String userId, ImageView iv, PlacesUtilCallback cbk) {
            this.userId = userId;
            this.imageView = iv;
            this.cbk = cbk;
        }

        @Override
        protected String doInBackground(String... uri) {
            URLConnection url;
            HttpURLConnection conn;
            BufferedReader rd;
            String line;
            String result = "";
            try {
                url = new URL(uri[0]).openConnection();
                conn = (HttpURLConnection) url;
                // conn.setRequestMethod("GET");
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    result += line;
                }
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException: " + e.getMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            String url = "";
            int size = 0;

            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONObject jsonObject1 = (JSONObject) jsonObject.get("image");
                url = jsonObject1.getString("url");
                String[] split = url.split("=");
                size = Integer.parseInt(split[1]);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            Log.d(TAG, "Profile pic JSON: " + s);
            Log.d(TAG, "Profile pic URL: " + url);
            Log.d(TAG, "Pic size: " + size);

            if (imageView != null && s != null && !s.equals(""))
                Picasso.with(PlacesApplication.getPlacesAppContext()).load(url).into(imageView);

            if (size == PlacesLoginUtils.LARGE_PIC_SIZE)
                PlacesLoginUtils.getInstance().addEntryToLargePicMap(userId, url);
            else PlacesLoginUtils.getInstance().addEntryToSmallPicMap(userId, url);

            if (cbk != null) cbk.onResult(url, null);
        }
    }

    class GetUsernameTask extends AsyncTask<String, String, String> {
        private String userId;
        private TextView tv;

        public GetUsernameTask(String userId, TextView tv) {
            this.userId = userId;
            this.tv = tv;
        }

        @Override
        protected String doInBackground(String... uri) {
            URLConnection url;
            HttpURLConnection conn;
            BufferedReader rd;
            String line;
            String result = "";
            try {
                url = new URL(uri[0]).openConnection();
                conn = (HttpURLConnection) url;
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    result += line;
                }
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException: " + e.getMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            String displayName = "";

            try {
                JSONObject jsonObject = new JSONObject(s);
                displayName = jsonObject.getString("displayName");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            tv.setText(displayName);
            PlacesLoginUtils.getInstance().addEntryToUserIdMap(userId, displayName);
        }
    }
}