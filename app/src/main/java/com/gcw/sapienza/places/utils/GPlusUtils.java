package com.gcw.sapienza.places.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.models.PlacesToken;
import com.gcw.sapienza.places.models.PlacesUser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.model.people.Person;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by mic_head on 27/02/15.
 */
public class GPlusUtils {

    private static final String TAG = "GPlusUtils";

    private static final GPlusUtils shared_instance = new GPlusUtils();

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private Person currentPerson;

    private static final String API_KEY = "AIzaSyCCWHhHY20hJqIzjMQfGnwfYDv1wU_W1GU";

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

    public void downloadGPlusInfo(Activity activity) {
        /**
         * Fetching user's information name, email, profile pic
         * */
        try
        {
            // We don't want to load Login screen, this is why the second parameter is false
            PlacesLoginUtils.startLoginActivity(activity, false);

            /*
            Person currentPerson = Plus.PeopleApi
                    .getCurrentPerson(mGoogleApiClient);
            String personName = currentPerson.getDisplayName();
            String personPhotoUrl = currentPerson.getImage().getUrl();
            String personGooglePlusProfile = currentPerson.getUrl();
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

            Log.e(TAG, "Name: " + personName + ", plusProfile: "
                    + personGooglePlusProfile + ", email: " + email
                    + ", Image: " + personPhotoUrl);


            // by default the profile url gives 50x50 px image only
            // we can replace the value with whatever dimension we want by
            // replacing sz=X
            personPhotoUrl = personPhotoUrl.substring(0,
                    personPhotoUrl.length() - 2)
                    + PlacesLoginUtils.LARGE_PIC_SIZE;

            PlacesLoginUtils.getInstance().addEntryToLargePicMap(currentPerson.getId(), personPhotoUrl);

            // LoadProfileImage lpi = new LoadProfileImage(currentPerson.getId());
            // lpi.execute();
            */
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error encountered while retrieving G+ info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GoogleApiClient getGoogleApiClient()
    {
        return mGoogleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient)
    {
        this.mGoogleApiClient = googleApiClient;
    }

    public Person getCurrentPerson()
    {
        return currentPerson;
    }

    public void setCurrentPerson(Person currentPerson)
    {
        this.currentPerson = currentPerson;
    }

    void parseResult(String result, String userId) throws JSONException
    {
        JSONObject mainObject = new JSONObject(result);
        JSONObject imageObject = mainObject.getJSONObject("image");
        String imageUrl = imageObject.getJSONObject("url").toString();

        PlacesLoginUtils.getInstance().addEntryToLargePicMap(userId, imageUrl);
        PlacesLoginUtils.getInstance().addEntryToSmallPicMap(userId, imageUrl);
    }

    @Deprecated
    public String getGPlusUsername()
    {
        return ((PlacesUser) ParseUser.getCurrentUser()).getName();
    }

    @Deprecated
    public void loadUsernameIntoTextView(String user_id, final TextView tv)
    {
        // Not implemented, and really no good reason to do it
    }

    public void getGPlusProfilePictureURL(final String user_id, final PlacesLoginUtils.PicSize size, ImageView iv, PlacesUtilCallback cbk)
    {
        String urlToRead = "https://www.googleapis.com/plus/v1/people/" + user_id + "?fields=image&sz=" + size + "&key=" + API_KEY;

        Log.d(TAG, "Request URL: " + urlToRead);

        GetPicTask gpt = new GetPicTask(user_id, iv, cbk);
        gpt.execute(urlToRead);
    }

    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size, PlacesUtilCallback cbk)
    {
        getGPlusProfilePictureURL(user_id, size, imageView, cbk);
    }

    /**
     * Background Async task to load user profile picture from url
     */
    @Deprecated
    private static class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {

        String userId;

        public LoadProfileImage(String userId) {
            this.userId = userId;
        }

        protected Bitmap doInBackground(String... urls)
        {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            //TODO
        }
    }

    @Deprecated
    class RequestTask extends AsyncTask<String, String, String> {

        private String userId;

        public RequestTask(String userId) {
            this.userId = userId;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                parseResult(result, userId);
            } catch (JSONException jsone) {
                Log.e(TAG, jsone.toString());
            }
        }
    }

    @Deprecated
    public static boolean checkGPlusTokenValidity()
    {
        PlacesUser user = (PlacesUser)ParseUser.getCurrentUser();
        ParseQuery<PlacesToken> query = ParseQuery.getQuery("TokenStorage");
        query.whereEqualTo("user", user);
        query.findInBackground(new FindCallback<PlacesToken>() {
            @Override
            public void done(List<PlacesToken> list, ParseException e)
            {
                if(e == null) Log.e(TAG, "Error encountered while retrieving access token");
                else if(list.size() != 0)
                {
                    // TODO
                }
            }
        });

        return false;
    }

    class GetPicTask extends AsyncTask<String, String, String>
    {
        private String userId;
        private ImageView imageView;
        private PlacesUtilCallback cbk;

        public GetPicTask(String userId, ImageView iv, PlacesUtilCallback cbk)
        {
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
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);

            String url = "";
            int size = 0;

            try
            {
                JSONObject jsonObject = new JSONObject(s);
                JSONObject jsonObject1 = (JSONObject)jsonObject.get("image");
                url = jsonObject1.getString("url");
                String[] split = url.split("=");
                size = Integer.parseInt(split[1]);
            }
            catch(JSONException e)
            {
                Log.e(TAG, e.getMessage());
            }

            Log.d(TAG, "Profile pic JSON: " + s);
            Log.d(TAG, "Profile pic URL: " + url);
            Log.d(TAG, "Pic size: " + size);

            if(imageView != null && s != null && !s.equals("")) Picasso.with(PlacesApplication.getPlacesAppContext()).load(url).into(imageView);

            if(size == PlacesLoginUtils.LARGE_PIC_SIZE) PlacesLoginUtils.getInstance().addEntryToLargePicMap(userId, url);
            else PlacesLoginUtils.getInstance().addEntryToSmallPicMap(userId, url);

            if(cbk!= null) cbk.onResult(url, null);
        }
    }
}