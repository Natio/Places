package com.gcw.sapienza.places.utils;

import android.app.ProgressDialog;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class FacebookUtils {

    private static final String TAG = "FacebookUtils";
    private static final FacebookUtils shared_instance = new FacebookUtils();
    private final HashMap<String, HashSet<PlacesUtilCallback>> scheduledOperationsQueue = new HashMap<>();

    //attempt to solve slow download of profile picture
    public static final String DEFAULT_AVATAR_URL = "http://1.gravatar.com/avatar/ad516503a11cd5ca435acc9bb6523536?s=256";

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
     * @return true if the current user is ready
     */
    public static boolean isFacebookSessionOpened() {
        return ParseFacebookUtils.getSession() != null && ParseFacebookUtils.getSession().isOpened();
    }

    public void downloadFacebookInfo(Context ctx) {
        final ProgressDialog progress = new ProgressDialog(ctx);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
//        not showing progress dialog
//        progress.show();
        FacebookUtils.getInstance().makeMeRequest(new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if (e != null) {
                    // FIXME result not being used?

                    Log.d(TAG, e.getMessage());
                    progress.setMessage(e.getMessage());
                } else {
                    progress.dismiss();
                    Log.d(TAG, result);
                }
            }
        });
    }

    /**
     * Configures current user
     *
     * @param cbk callback
     */
    public void makeMeRequest(final PlacesUtilCallback cbk) {

        final Session session = ParseFacebookUtils.getSession();
        if (session == null) {
            cbk.onResult(null, new RuntimeException("Session not valid"));
            return;
        }

        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            PlacesLoginUtils.getInstance().setCurrentUserId(user.getId());
                            PlacesLoginUtils.getInstance().addEntryToUserIdMap(user.getId(), user.getUsername());

                            FacebookUtils.this.fetchFbFriends(new FacebookUtilsFriendsCallback() {
                                @Override
                                public void onFriendsResult(List<String> friends, Exception e) {
                                    if (cbk != null) {
                                        cbk.onResult(PlacesLoginUtils.getInstance().getCurrentUserId(), e);
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
        PlacesLoginUtils.getInstance().clearFriends();

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
                                PlacesLoginUtils.getInstance().addFriend(((JSONObject) array.get(i)).getString("id"));
                            }
                            if (cbk != null) {
                                cbk.onFriendsResult(PlacesLoginUtils.getInstance().getFriends(), null);
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
     * WARNING: FETCHING FRIENDS' FRIENDS IS NOT CURRENTLY SUPPORTED,
     * THEREFORE AT THIS TIME THIS FUNCTION DOESN'T WORK AS EXPECTED
     *
     * @param fbId Facebook id of the friend
     * @param cbk  callback to be called when response is received
     */
    public void getFriendsFriends(String fbId, final FacebookUtilsFriendsCallback cbk) {
        Bundle bundle = new Bundle();
        bundle.putString("fields", "id");

        final Session session = ParseFacebookUtils.getSession();

        Request req = new Request(session, fbId + "/friends", bundle, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        try {
                            GraphObject go = response.getGraphObject();
                            JSONObject obj = go.getInnerJSONObject();

                            Log.v(TAG, "FB friends: " + obj.toString());

                            JSONArray array = obj.getJSONArray("data");
                            List<String> friendsFriends = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                friendsFriends.add(((JSONObject) array.get(i)).getString("id"));
                            }
                            if (cbk != null) {
                                cbk.onFriendsResult(friendsFriends, null);
                            }
                        } catch (JSONException e) {
                            Log.v(TAG, "Couldn't retrieve friend's friends.  Error: " + e.toString());
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
    public void getFacebookUsernameFromID(final String fb_id, final PlacesUtilCallback cbk) {
        Log.d(TAG, "In getFacebookUsernameFromID");

        String username = PlacesLoginUtils.getInstance().getUserNameFromId(fb_id);
        if (username != null) {
            if (cbk != null) {
                cbk.onResult(username, null);
                return;
            }
        }

        final String current_key = "NAME " + fb_id;
        synchronized (this.scheduledOperationsQueue) {
            if (this.scheduledOperationsQueue.containsKey(current_key)) {
                Set<PlacesUtilCallback> cbks = this.scheduledOperationsQueue.get(current_key);
                cbks.add(cbk);
                //Log.d(TAG, "Enqueued"+current_key);
                return;
            } else {
                HashSet<PlacesUtilCallback> newSet = new HashSet<>();
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

                            Log.d(TAG, "Response in getFacebookUsernameFromID: " + go.toString());

                            JSONObject obj = go.getInnerJSONObject();
                            String name = obj.getString("name");

                            PlacesLoginUtils.getInstance().addEntryToUserIdMap(fb_id, name);

                            Set<PlacesUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue) {
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }


                            if (cbks != null) {
                                for (PlacesUtilCallback c : cbks) {
                                    c.onResult(name, null);
                                }
                            }
                        } catch (JSONException | NullPointerException e) {
                            Log.v(TAG, "Couldn't resolve facebook user's name.  Error: " + e.toString());
                            e.printStackTrace();
                            Set<PlacesUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue) {
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }
                            if (cbks != null) {
                                for (PlacesUtilCallback c : cbks) {
                                    c.onResult(null, e);
                                }
                            }
                        }
                    }
                });

        req.executeAsync();
    }

    protected void loadUsernameIntoTextView(final String userId, final TextView tv) {
        // if(PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.FACEBOOK) {
        getFacebookUsernameFromID(userId, new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                Log.d(TAG, "Result in loadUsernameIntoTextView: " + result);
                tv.setText(result);
            }
        });
        /*}
        else
        {
            Bundle bundle = new Bundle();
            bundle.putString("fields", "name");
            Request req = new Request(ParseFacebookUtils.getSession(), userId, bundle, HttpMethod.GET,
                    new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            try
                            {
                                GraphObject go = response.getGraphObject();

                                JSONObject obj = go.getInnerJSONObject();
                                String name = obj.getString("name");

                                PlacesLoginUtils.getInstance().addEntryToUserIdMap(userId, name);
                                tv.setText(name);

                            }
                            catch (JSONException | NullPointerException e)
                            {
                                Log.v(TAG, "Couldn't resolve facebook user's name.  Error: " + e.toString());
                                e.printStackTrace();
                            }
                        }
                    });

            req.executeAsync();
        }*/
    }

    /**
     * Asynchronously loads a profile pictures into an image view
     *
     * @param user_id   facebook user id
     * @param imageView ImageView where to load picture
     */
    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size) {

        this.getFbProfilePictureURL(user_id, size, new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if (e == null) {
                    if (result.trim().length() == 0) {
                        Picasso.with(PlacesApplication.getPlacesAppContext()).load(DEFAULT_AVATAR_URL).into(imageView);
                    } else {
                        Picasso.with(PlacesApplication.getPlacesAppContext()).load(result).into(imageView);
                    }
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
    public void getFbProfilePictureURL(final String user_id, final PlacesLoginUtils.PicSize size, final PlacesUtilCallback cbk) {


        String pic_url = PlacesLoginUtils.getInstance().getProfilePictureURL(user_id, size);
        if (pic_url != null) {
            cbk.onResult(pic_url, null);
            return;
        }


        final String current_key = "PIC_" + size + '_' + user_id;
        synchronized (this.scheduledOperationsQueue) {

            if (this.scheduledOperationsQueue.containsKey(current_key)) {
                Set<PlacesUtilCallback> cbksSet = this.scheduledOperationsQueue.get(current_key);
                cbksSet.add(cbk);
                //Log.d(TAG, "Enqueued: " + user_id);
                return;
            } else {
                HashSet<PlacesUtilCallback> cbksSet = new HashSet<>();
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

                            if (size == PlacesLoginUtils.PicSize.SMALL) {
                                PlacesLoginUtils.getInstance().addEntryToSmallPicMap(user_id, url);
                            } else {
                                PlacesLoginUtils.getInstance().addEntryToLargePicMap(user_id, url);
                            }

                            Set<PlacesUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue) {
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }

                            if (cbks != null) {
                                for (PlacesUtilCallback c : cbks) {
                                    c.onResult(url, null);
                                }
                            }

                        } catch (JSONException e) {
                            Log.v(TAG, "Couldn't retrieve facebook user data.  Error: " + e.toString());
                            e.printStackTrace();
                            Set<PlacesUtilCallback> cbks;
                            synchronized (FacebookUtils.this.scheduledOperationsQueue) {
                                cbks = FacebookUtils.this.scheduledOperationsQueue.remove(current_key);
                            }
                            if (cbks != null) {
                                for (PlacesUtilCallback c : cbks) {
                                    c.onResult(null, e);
                                }
                            }

                        } catch (NullPointerException npe) {
                            Log.e(TAG, "GraphObject is null!");
                            npe.printStackTrace();
                        }
                    }
                }
        );

        req.executeAsync();
    }
}