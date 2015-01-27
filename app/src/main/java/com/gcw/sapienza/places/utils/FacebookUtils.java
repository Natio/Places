package com.gcw.sapienza.places.utils;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.gcw.sapienza.places.PlacesApplication;
import com.parse.ParseFacebookUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class FacebookUtils {
    private static final String TAG = "FacebookUtils";

    public enum PicSize {
        SMALL, LARGE;

        public String toString() {
            if (this == SMALL) {
                return SMALL_PIC_SIZE;
            }
            return LARGE_PIC_SIZE;
        }
    }

    private static final String LARGE_PIC_SIZE = "200";
    private static final String SMALL_PIC_SIZE = "120";

    private String fbId = "";
    private final ArrayList<String> friends = new ArrayList<>();
    private final HashMap<String, String> userIdMap = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapSmall = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapLarge = new HashMap<>();

    private final HashMap<String, HashSet<FacebookUtilCallback>> scheduledOperationsQueue = new HashMap<>();

    private static final FacebookUtils shared_instance = new FacebookUtils();

    private FacebookUtils() {
    }

    /**
     * This is a singleton class. This method returns the ONLY instance
     *
     * @return Singleton instance
     */
    public static FacebookUtils getInstance() {
        return FacebookUtils.shared_instance;
    }


    /**
     * Removes all user data
     */
    public void clearUserData() {
        this.fbId = "";
        this.friends.clear();
        this.userIdMap.clear();
        this.userProfilePicMapSmall.clear();
        this.userProfilePicMapLarge.clear();
    }

    /**
     * @return current user's fb id
     */
    public String getCurrentUserId() {
        return this.fbId;
    }


    /**
     * @return true if there is a valid facebook id for the current user
     */
    public boolean hasCurrentUserId() {
        return !(this.fbId == null || this.fbId.isEmpty());
    }

    /**
     * Returns  fb user name for a given id
     *
     * @param id the facebook id of a user
     * @return fb user name for a given id
     */
    private String getUserNameFromId(String id) {
        return this.userIdMap.get(id);
    }

    public String getProfilePictureSmall(String profile_id) {
        return this.userProfilePicMapSmall.get(profile_id);
    }

    public String getProfilePictureLarge(String profile_id) {
        return this.userProfilePicMapLarge.get(profile_id);
    }

    /**
     * @param profile_id profile idenrifier
     * @param size       Site of the picture
     * @return cached URL of profile_id profile picture. Returns null if the picture is not cached
     */
    public String getProfilePictureURL(String profile_id, PicSize size) {
        if (size == PicSize.LARGE) {
            return this.getProfilePictureLarge(profile_id);
        } else if (size == PicSize.SMALL) {
            return this.getProfilePictureSmall(profile_id);
        }
        throw new InvalidParameterException("wrong size specified: " + size);
    }

    /**
     * @return current user's friend list
     */
    public List<String> getFriends() {
        return this.friends;
    }

    /**
     * Configures current user
     *
     * @param cbk callback
     */
    public void makeMeRequest(final FacebookUtilCallback cbk) {

        final Session session = ParseFacebookUtils.getSession();
        if (session == null){
            cbk.onResult(null, new RuntimeException("Session not valid"));
            return;
        }

        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            FacebookUtils.this.fbId = user.getId();
                            FacebookUtils.this.userIdMap.put(user.getId(), user.getUsername());

                            FacebookUtils.this.fetchFbFriends(new FacebookUtilsFriendsCallback() {
                                @Override
                                public void onFriendsResult(List<String> friends, Exception e) {
                                    if (cbk != null) {
                                        cbk.onResult(FacebookUtils.this.fbId, e);
                                    }
                                }
                            });


                        }
                    }
                });
        request.executeAsync();
    }

    /**
     * Asynchronously fetches current user's facebook friends
     *
     * @param cbk callback
     */
    public void fetchFbFriends(final FacebookUtilsFriendsCallback cbk) {
        this.friends.clear();

        Bundle bundle = new Bundle();
        bundle.putString("fields", "id");

        final Session session = ParseFacebookUtils.getSession();

        Request req = new Request(session, "me/friends", bundle, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        try {
                            GraphObject go = response.getGraphObject();
                            JSONObject obj = go.getInnerJSONObject();

                            Log.v(TAG, "FB friends: " + obj.toString());

                            JSONArray array = obj.getJSONArray("data");

                            for (int i = 0; i < array.length(); i++) {
                                FacebookUtils.this.friends.add(((JSONObject) array.get(i)).getString("id"));
                            }
                            if (cbk != null) {
                                cbk.onFriendsResult(FacebookUtils.this.friends, null);
                            }
                        } catch (JSONException e) {
                            Log.v(TAG, "Couldn't retrieve user's friends.  Error: " + e.toString());
                            e.printStackTrace();
                            if (cbk != null) {
                                cbk.onFriendsResult(null, e);
                            }
                        }
                    }
                }
        );

        req.executeAsync();
    }

    /**
     * Asynchronously retrieves the User's username. If the username is cached callback will be called immediately.
     * Otherwise a request to fb APIs will be issued
     *
     * @param fb_id FB user id
     * @param cbk   callback parameter. MUST not be null. User Username will be given as a parameter of onResult method
     */
    public void getFacebookUsernameFromID(final String fb_id, final FacebookUtilCallback cbk) {
        String username = this.getUserNameFromId(fb_id);
        if (username != null) {
            if (cbk != null) {
                cbk.onResult(username, null);
                return;
            }
        }

        final String current_key = "NAME " + fb_id;
        synchronized (this.scheduledOperationsQueue){
            if(this.scheduledOperationsQueue.containsKey(current_key)){
                Set<FacebookUtilCallback> cbks = this.scheduledOperationsQueue.get(current_key);
                cbks.add(cbk);
                //Log.d(TAG, "Enqueued"+current_key);
                return;
            }
            else{
                HashSet<FacebookUtilCallback> newSet = new HashSet<>();
                newSet.add(cbk);
                this.scheduledOperationsQueue.put(current_key, newSet);
                //Log.d(TAG, "Scheduled"+current_key);
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString("fields", "name");
        Request req = new Request(ParseFacebookUtils.getSession(), fb_id, bundle, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        try {
                            GraphObject go = response.getGraphObject();

                            JSONObject obj = go.getInnerJSONObject();
                            String name = obj.getString("name");

                            FacebookUtils.this.userIdMap.put(fb_id, name);

                            Set<FacebookUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue){
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }


                            if (cbks != null) {
                                for(FacebookUtilCallback c : cbks){
                                    c.onResult(name, null);
                                }
                            }
                        } catch (JSONException | NullPointerException e) {
                            Log.v(TAG, "Couldn't resolve facebook user's name.  Error: " + e.toString());
                            e.printStackTrace();
                            Set<FacebookUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue){
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }
                            if (cbks != null) {
                                for(FacebookUtilCallback c : cbks){
                                    c.onResult(null, e);
                                }
                            }
                        }
                    }
                });

        req.executeAsync();

    }

    /**
     * Asynchronously sets a facebook username into a TextView instance.
     * If username is cached this method will immediately set the username,
     * otherwise a request to facebook APIs will be issued
     *
     * @param fb_id facebook id of user
     * @param tv    the TextView instance where to load the username
     */
    public void loadUsernameIntoTextView(String fb_id, final TextView tv) {
        this.getFacebookUsernameFromID(fb_id, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if (e == null) {
                    tv.setText(result);
                } else {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }


    /**
     * Asynchronously loads a profile pictures into an image view
     *
     * @param user_id   facebook user id
     * @param imageView ImageView where to load picture
     */
    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PicSize size) {
        this.getFbProfilePictureURL(user_id, size, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if (e == null) {
                    Picasso.with(PlacesApplication.getPlacesAppContext()).load(result).into(imageView);
                } else {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * Asynchronously computes the url of a FB profile picture
     *
     * @param user_id FB user id
     * @param size    size of the profile picture
     * @param cbk     callback parameter. MUST not be null. Picture URL will be given as a parameter of onResult method
     */
    public void getFbProfilePictureURL(final String user_id, final PicSize size, final FacebookUtilCallback cbk) {
        String pic_url = this.getProfilePictureURL(user_id, size);

        if (pic_url != null) {
            cbk.onResult(pic_url, null);
            return;
        }

        final String current_key = "PIC_" + size + '_' + user_id;
        synchronized (this.scheduledOperationsQueue){

            if(this.scheduledOperationsQueue.containsKey(current_key)){
                Set<FacebookUtilCallback> cbksSet = this.scheduledOperationsQueue.get(current_key);
                cbksSet.add(cbk);
                //Log.d(TAG, "Enqueued: " + user_id);
                return;
            }
            else{
                HashSet<FacebookUtilCallback> cbksSet = new HashSet<>();
                cbksSet.add(cbk);
                //Log.d(TAG, "Scheduled: " + user_id);
                this.scheduledOperationsQueue.put(current_key, cbksSet);
            }
        }


        Bundle bundle = new Bundle();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", size.toString());
        bundle.putString("type", "normal");
        bundle.putString("width", size.toString());

        Request req = new Request(ParseFacebookUtils.getSession(), '/' + user_id + "/picture", bundle, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        try {
                            GraphObject go = response.getGraphObject();

                            JSONObject obj = go.getInnerJSONObject();
                            final String url = obj.getJSONObject("data").getString("url");

                            if (size == PicSize.SMALL) {
                                FacebookUtils.this.userProfilePicMapSmall.put(user_id, url);
                            } else {
                                FacebookUtils.this.userProfilePicMapLarge.put(user_id, url);
                            }

                            Set<FacebookUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue){
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }

                            if(cbks != null){
                                for (FacebookUtilCallback c : cbks){
                                    c.onResult(url, null);
                                }
                            }


                        } catch (JSONException e) {
                            Log.v(TAG, "Couldn't retrieve facebook user data.  Error: " + e.toString());
                            e.printStackTrace();
                            Set<FacebookUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue){
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }
                            if(cbks != null){
                                for (FacebookUtilCallback c : cbks){
                                    c.onResult(null, e);
                                }
                            }

                        }
                    }
                }
        );

        req.executeAsync();
    }

}