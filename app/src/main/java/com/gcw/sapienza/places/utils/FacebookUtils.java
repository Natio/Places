package com.gcw.sapienza.places.utils;

import android.os.Bundle;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.parse.ParseFacebookUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public final class FacebookUtils {
    private static final String TAG = "FacebookUtils";

    public static final String LARGE_PIC_SIZE = "200";
    public static final String SMALL_PIC_SIZE = "120";

    private String fbId = "";
    private final ArrayList<String> friends = new ArrayList<>();
    private final HashMap<String, String> userIdMap = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapSmall = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapLarge = new HashMap<>();

    private static final FacebookUtils shared_instance = new FacebookUtils();

    private FacebookUtils(){}

    public static FacebookUtils getInstance(){
        return FacebookUtils.shared_instance;
    }


    public void clearUserData()
    {
        this.fbId = "";
        this.friends.clear();
        this.userIdMap.clear();
        this.userProfilePicMapSmall.clear();
        this.userProfilePicMapLarge.clear();
    }

    public String getCurrentUserId(){
        return this.fbId;
    }

    public void purgeDataOnLowMemory(){
        this.userProfilePicMapLarge.clear();
        this.userProfilePicMapSmall.clear();
    }

    /**
     * Returns  fb user name for a given id
     * @param id the facebook id of a user
     * @return
     */
    public String getUserNameFromId(String id){
        return this.userIdMap.get(id);
    }

    /**
     *
     * @return true if there is a valid facebook id for the current user
     */
    public boolean hasCurrentUserId(){
        return !(this.fbId == null || this.fbId.equals(""));
    }

    public String getProfilePictureSmall(String profile_id){
        return this.userProfilePicMapSmall.get(profile_id);
    }

    public String getProfilePictureLarge(String profile_id) {
        return this.userProfilePicMapLarge.get(profile_id);
    }

    public List<String> getFriends(){
        return this.friends;
    }

    public void fetchFbUsername(final String id, final FacebookUtilCallback callback) {
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
                            Log.d(TAG, ""+go);
                            Log.d(TAG, ""+id);
                            JSONObject obj = go.getInnerJSONObject();
                            String name = obj.getString("name");
                            FacebookUtils.this.userIdMap.put(id, name);
                            if(callback!= null){
                                callback.onResult(name, null);
                            }
                        }
                        catch(JSONException e)
                        {
                            Log.v(TAG, "Couldn't resolve facebook user's name.  Error: " + e.toString());
                            e.printStackTrace();
                            if(callback!= null){
                                callback.onResult(null, e);
                            }
                        }
                    }
                });

        req.executeAsync();
    }

    public synchronized void fetchFbProfilePic(final String id, final String size, final FacebookUtilCallback cbk) throws MalformedURLException, IOException
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

                            if(size.equals(SMALL_PIC_SIZE)) {
                                FacebookUtils.this.userProfilePicMapSmall.put(id, url);
                            }
                            else {
                                FacebookUtils.this.userProfilePicMapLarge.put(id, url);
                            }
                            if(cbk != null){
                                cbk.onResult(url, null);
                            }
                        }
                        catch(JSONException e)
                        {
                            Log.v(TAG, "Couldn't retrieve facebook user data.  Error: " + e.toString());
                            e.printStackTrace();
                            if(cbk != null){
                                cbk.onResult(null, e);
                            }
                        }
                    }
                }
        );

        req.executeAsync();
    }

    public void fetchFbFriends(final FacebookUtilsFriendsCallback cbk) throws  IOException, MalformedURLException
    {
        this.friends.clear();

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

                            for(int i = 0; i < array.length(); i++){
                                FacebookUtils.this.friends.add(((JSONObject)array.get(i)).getString("id"));
                            }
                            if (cbk != null){
                                cbk.onFriendsResult(FacebookUtils.this.friends, null);
                            }
                        }
                        catch(JSONException e)
                        {
                            Log.v(TAG, "Couldn't retrieve user's friends.  Error: " + e.toString());
                            e.printStackTrace();
                            if (cbk != null){
                                cbk.onFriendsResult(null, e);
                            }
                        }
                    }
                }
        );

        req.executeAsync();
    }


    public void makeMeRequest(final FacebookUtilCallback cbk){

        final Session session = ParseFacebookUtils.getSession();

        if(session == null) return;

        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback()
                {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            FacebookUtils.this.fbId = user.getId();
                            try
                            {
                                FacebookUtils.this.fetchFbFriends(new FacebookUtilsFriendsCallback() {
                                    @Override
                                    public void onFriendsResult(List<String> friends, Exception e) {
                                        if(cbk != null){
                                            cbk.onResult(FacebookUtils.this.fbId, e);
                                        }
                                    }
                                });
                            }
                            catch(IOException mue){
                                mue.printStackTrace();
                                if(cbk != null){
                                    cbk.onResult(null, mue);
                                }
                            }

                        }
                    }
                });
        request.executeAsync();
    }

}
