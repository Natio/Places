package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.models.PlacesUser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.parse.ParseUser;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mic_head on 27/02/15.
 */
public class GPlusUtils {

    private static final String TAG = "GPlusUtils";

    private static final GPlusUtils shared_instance = new GPlusUtils();

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private Person currentPerson;

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

    public static void downloadGPlusInfo(GoogleApiClient mGoogleApiClient, Context context) {
        /**
         * Fetching user's information name, email, profile pic
         * */
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
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

            } else {
                Toast.makeText(context, "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void getProfilePicFromUserId(String userId) {
        String request = "https://www.googleapis.com/plus/v1/people/";
        request += userId;
        request += "?fields=image&key=";
        request += "AIzaSyALZgcm_3X4_KmZg8ax6MmDLGFzZxE6c7Y";

        new RequestTask(userId).execute("");
    }

    void parseResult(String result, String userId) throws JSONException {
        JSONObject mainObject = new JSONObject(result);
        JSONObject imageObject = mainObject.getJSONObject("image");
        String imageUrl = imageObject.getJSONObject("url").toString();

        PlacesLoginUtils.getInstance().addEntryToLargePicMap(userId, imageUrl);
    }

    @Deprecated
    public String getGPlusUsername() {
        return ((PlacesUser) ParseUser.getCurrentUser()).getName();
    }

    @Deprecated
    public void loadUsernameIntoTextView(String user_id, final TextView tv) {

    }

    public void getGPlusProfilePictureURL(final String user_id, final PlacesLoginUtils.PicSize size, final FacebookUtilCallback cbk) {

    }

    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size) {

    }

    /**
     * Background Async task to load user profile picture from url
     */
    private static class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {

        String userId;

        public LoadProfileImage(String userId) {
            this.userId = userId;
        }

        protected Bitmap doInBackground(String... urls) {
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
}
