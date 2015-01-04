package com.gcw.sapienza.places.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.gcw.sapienza.places.MainActivity;
import com.parse.ParseFacebookUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mic_head on 02/01/15.
 */
public class Utils
{
    private static final String TAG = "Utils";

    public static String fbId = "";
    public static ArrayList<String> friends = new ArrayList<String>();
    public static HashMap<String, String> userIdMap = new HashMap<>();
    public static HashMap<String, String> userProfilePicMapSmall = new HashMap<>();
    public static HashMap<String, String> userProfilePicMapLarge = new HashMap<>();

    public static final int UPDATE_DELAY = 200;
    public static float MAP_RADIUS = 0.5f;
    public static int MAX_PINS = 10;

    public static int[] stepValues = {1, 5, 10, 15, 20};

    public static final String LARGE_PIC_SIZE = "200";
    public static final String SMALL_PIC_SIZE = "120";

    public static boolean LONE_WOLF_ENABLED = true;
    public static boolean WITH_FRIENDS_SURROUNDED_ENABLED = true;
    public static boolean STORYTELLERS_IN_THE_DARK_ENABLED = true;
    public static boolean ARCHAEOLOGIST_ENABLED = true;

    public static final int PIC_CAPTURE_REQUEST_CODE = 91;

    public static Activity mainActivity;

    public static void clearUserData()
    {
        fbId = "";
        friends.clear();
        userIdMap.clear();
        userProfilePicMapSmall.clear();
        userProfilePicMapLarge.clear();
    }

    public static void fetchFbUsername(final String id)
    {
        Bundle bundle = new Bundle();
        bundle.putString("fields", "name");

        Request req = new Request(ParseFacebookUtils.getSession(), id, bundle, HttpMethod.GET,
                new Request.Callback()
                {
                    @Override
                    public void onCompleted(Response response) {
                        try
                        {
                            GraphObject go = response.getGraphObject();

                            JSONObject obj = go.getInnerJSONObject();
                            userIdMap.put(id, obj.getString("name"));
                        }
                        catch(JSONException e)
                        {
                            Log.v(TAG, "Couldn't resolve facebook user's name.  Error: " + e.toString());
                            e.printStackTrace();
                        };
                    }
                });

        req.executeAsync();
    }

    public static void fetchFbProfilePic(final String id, final String size) throws MalformedURLException, IOException
    {
        Bundle bundle = new Bundle();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", size);
        bundle.putString("type", "normal");
        bundle.putString("width", size);

        Request req = new Request(ParseFacebookUtils.getSession(), "/"+id+"/picture", bundle, HttpMethod.GET,
                new Request.Callback()
                {
                    @Override
                    public void onCompleted(Response response) {
                        try
                        {
                            GraphObject go = response.getGraphObject();

                            JSONObject obj = go.getInnerJSONObject();
                            final String url = obj.getJSONObject("data").getString("url");

                            if(size.equals(SMALL_PIC_SIZE)) userProfilePicMapSmall.put(id, url);
                            else userProfilePicMapLarge.put(id, url);
                        }
                        catch(JSONException e)
                        {
                            Log.v(TAG, "Couldn't retrieve facebook user data.  Error: " + e.toString());
                            e.printStackTrace();
                        };
                    }
                }
        );

        req.executeAsync();
    }

    public static void fetchFbFriends() throws MalformedURLException, IOException
    {
        friends.clear();

        Bundle bundle = new Bundle();
        bundle.putString("fields", "id");

        final Session session = ParseFacebookUtils.getSession();

        Request req = new Request(session, "me/friends", bundle, HttpMethod.GET,
            new Request.Callback()
            {
                @Override
                public void onCompleted(Response response)
                {
                    try
                    {
                        GraphObject go = response.getGraphObject();
                        JSONObject obj = go.getInnerJSONObject();

                        Log.v(TAG, "FB friends: " + obj.toString());

                        JSONArray array = obj.getJSONArray("data");

                        for(int i = 0; i < array.length(); i++) friends.add(((JSONObject)array.get(i)).getString("id"));
                    }
                    catch(JSONException e)
                    {
                        Log.v(TAG, "Couldn't retrieve user's friends.  Error: " + e.toString());
                        e.printStackTrace();
                    };
                }
            }
        );

        req.executeAsync();
    }

    @Deprecated // Daniele says its 'getLocation' is much better than mine, that's why it's deprecated.
    public static Location getLocation(Context context)
    {
        Location location;
        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled || isNetworkEnabled)
        {
            if (isNetworkEnabled)
            {
                if (locationManager != null)
                {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    return location;
                }
            }
            if (isGPSEnabled)
            {
                if (locationManager != null)
                {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    return location;
                }
            }
        }
        else Toast.makeText(context, "Please enable GPS data", Toast.LENGTH_LONG).show();

        return null;
    }

    public static void makeMeRequest()
    {
        final Session session = ParseFacebookUtils.getSession();

        if(session == null) return;

        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback()
                {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            Utils.fbId = user.getId();
                            try
                            {
                                Utils.fetchFbFriends();
                            }
                            catch(MalformedURLException mue){ mue.printStackTrace(); }
                            catch(IOException ioe){ ioe.printStackTrace(); }
                        }
                    }
                });
        request.executeAsync();
    }

    public static void updatePreferences(Context context)
    {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        Utils.LONE_WOLF_ENABLED = preferences.getBoolean("meFilter", true);
        Utils.WITH_FRIENDS_SURROUNDED_ENABLED = preferences.getBoolean("flFilter", true);
        Utils.STORYTELLERS_IN_THE_DARK_ENABLED = preferences.getBoolean("strangersFilter", true);
        Utils.ARCHAEOLOGIST_ENABLED = preferences.getBoolean("timeFilter", true);
    }
}
