package com.gcw.sapienza.places.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.activities.PlacesLoginActivity;
import com.gcw.sapienza.places.models.PlacesUser;
import com.google.android.gms.plus.Plus;
import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mic_head on 27/02/15.
 */
public class PlacesLoginUtils {

    protected static final int LARGE_PIC_SIZE = 200;
    protected static final int SMALL_PIC_SIZE = 120;
    public static final String GPLUS_TOKEN_SP = "G+_TOKEN";
    public static final String EMAIL_SP = "email";

    private static final String TAG = "PlacesLoginUtils";

    private static final PlacesLoginUtils shared_instance = new PlacesLoginUtils();
    public static LoginType loginType;
    private final ArrayList<String> friends = new ArrayList<>();
    private final HashMap<String, String> userIdMap = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapSmall = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapLarge = new HashMap<>();
    private String userId;
    private PlacesLoginUtils() {
    }

    /**
     * This is a singleton class. This method returns the ONLY instance
     *
     * @return Singleton instance
     */
    public static PlacesLoginUtils getInstance() {
        return PlacesLoginUtils.shared_instance;
    }

    public static void downloadUserInfo(Activity activity)
    {
        if (loginType == LoginType.FACEBOOK) FacebookUtils.getInstance().downloadFacebookInfo(activity);
        else if (loginType == LoginType.GPLUS) GPlusUtils.getInstance().downloadGPlusInfo(activity);
    }

    public String getCurrentUserId() {
        return this.userId;
    }

    public void setCurrentUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<String> getFriends() {
        return this.friends;
    }

    public void setFriends(ArrayList<String> friends) {
        this.friends.addAll(friends);
    }

    public HashMap<String, String> getUserIdMap() {
        return this.userIdMap;
    }

    public HashMap<String, String> getUserProfilePicMapSmall() {
        return this.getUserProfilePicMapSmall();
    }

    public HashMap<String, String> getUserProfilePicMapLarge() {
        return this.getUserProfilePicMapLarge();
    }

    public static void startLoginActivity(Activity activity, boolean canChoose)
    {
        PlacesLoginBuilder builder = new PlacesLoginBuilder(activity);

        // builder.setAppLogo(R.drawable.app_logo);
        Intent loginIntent = builder.build();
        loginIntent.setClass(activity, PlacesLoginActivity.class);
        loginIntent.putExtra("canChoose", canChoose);
        // loginIntent.setClass(activity, ParseLoginActivity.class);

        activity.startActivityForResult(loginIntent, Utils.LOGIN_REQUEST_CODE);
    }

    public void addFriend(String friend) {
        this.friends.add(friend);
    }

    public void addEntryToUserIdMap(String key, String value) {
        this.userIdMap.put(key, value);
    }

    public void addEntryToSmallPicMap(String key, String value) {
        this.userProfilePicMapSmall.put(key, value);
    }

    public void addEntryToLargePicMap(String key, String value) {
        this.userProfilePicMapLarge.put(key, value);
    }

    /**
     * Removes all user data
     */
    public void clearUserData() {
        this.userId = "";
        this.friends.clear();
        this.userIdMap.clear();
        this.userProfilePicMapSmall.clear();
        this.userProfilePicMapLarge.clear();
    }

    public void clearFriends() {
        this.friends.clear();
    }

    public void clearUserIdMap() {
        this.userIdMap.clear();
    }

    public void clearUserProfilePicMapSmall() {
        this.userProfilePicMapSmall.clear();
    }

    public void clearUserProfilePicMapLarge() {
        this.userProfilePicMapLarge.clear();
    }

    /**
     * @return true if there is a valid user id for the current user
     */
    public boolean hasCurrentUserId() {
        return !(this.userId == null || this.userId.isEmpty());
    }

    /**
     * Returns  fb user name for a given id
     *
     * @param id the facebook id of a user
     * @return fb user name for a given id
     */
    public String getUserNameFromId(String id) {
        return this.userIdMap.get(id);
    }

    public String getProfilePictureSmall(String profile_id) {
        return this.userProfilePicMapSmall.get(profile_id);
    }

    public String getProfilePictureLarge(String profile_id) {
        return this.userProfilePicMapLarge.get(profile_id);
    }

    public void checkForSessionValidityAndStartDownloadingInfo(Activity context)
    {
        String googleToken = "";
        String email = "";

        if(ParseUser.getCurrentUser() != null)
        {
            if (FacebookUtils.isFacebookSessionOpened())
            {
                loginType = LoginType.FACEBOOK;

                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(GPLUS_TOKEN_SP, "").commit();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(EMAIL_SP, "").commit();

                PlacesLoginUtils.getInstance().downloadUserInfo(context);
            }
            else
            {
                loginType = LoginType.GPLUS;

                googleToken = PreferenceManager.getDefaultSharedPreferences(context).getString(GPLUS_TOKEN_SP, "");
                email = PreferenceManager.getDefaultSharedPreferences(context).getString(EMAIL_SP, "");

                Log.d(TAG, "Token retrieved in PlacesLoginUtils: " + googleToken);
                Log.d(TAG, "Email retrieved in PlacesLoginUtils: " + email);

                if(!googleToken.equals("") && !email.equals("")) tryToLoginWithAvailableToken(context, googleToken, email);
                else PlacesLoginUtils.startLoginActivity(context, true);
            }
        }
        else
        {
            Log.d(TAG, "currentUser is null");

            PlacesLoginUtils.startLoginActivity(context, true);
        }
    }

    private void tryToLoginWithAvailableToken(final Activity context, final String token, String email)
    {
        final HashMap<String, Object> params = new HashMap();
        params.put("code", token);
        params.put("email", email);

        Log.d(TAG, "Calling cloud code for authentication...");

        //loads the Cloud function to create a Google user
        ParseCloud.callFunctionInBackground("accessGoogleUser", params, new FunctionCallback<Object>() {
            @Override
            public void done(final Object returnObj, ParseException e)
            {
                if (e == null)
                {
                    ParseUser.becomeInBackground(returnObj.toString(), new LogInCallback()
                    {
                        public void done(ParseUser user, ParseException e)
                        {
                            Log.d(TAG, "So that's the token: " + token);

                            if (user != null && e == null)
                            {
                                 Log.d(TAG, "The Google user validated - WAT");

                                 GPlusUtils.getInstance().setGoogleApiClient(GPlusUtils.getInstance().getGoogleApiClient());
                                 PlacesLoginUtils.downloadUserInfo(context);
                            }
                            else if (e != null)
                            {
                                // Toast.makeText(context, "There was a problem creating your account. - WAT", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "There was a problem creating your account. - WAT");
                                e.printStackTrace();
                                GPlusUtils.getInstance().getGoogleApiClient().disconnect();

                                // Token was probably outdated
                                startLoginActivity(context, true);

                            }
                            else
                            {
                                Log.d(TAG, "The Google token could not be validated - WAT");

                                // Token was probably outdated
                                startLoginActivity(context, true);
                            }
                        }
                    });
                }
                else
                {
                    // Toast.makeText(context, "There was a problem creating your account. - WAT", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "There was a problem creating your account. - WAT: "  + e.getMessage());
                    e.printStackTrace();
                    // if(GPlusUtils.getInstance().getGoogleApiClient() != null) GPlusUtils.getInstance().getGoogleApiClient().disconnect();

                    // Token was probably outdated
                    startLoginActivity(context, true);
                }
            }
        });
    }

    /**
     * @param profile_id profile identifier
     * @param size       Site of the picture
     * @return cached URL of profile_id profile picture. Returns null if the picture is not cached
     */
    public String getProfilePictureURL(String profile_id, PlacesLoginUtils.PicSize size) {
        if (size == PlacesLoginUtils.PicSize.LARGE) {
            return this.getProfilePictureLarge(profile_id);
        } else if (size == PlacesLoginUtils.PicSize.SMALL) {
            return this.getProfilePictureSmall(profile_id);
        }
        throw new InvalidParameterException("wrong size specified: " + size);
    }

    public void loadUsernameIntoTextView(final String userId, final TextView tv)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.getInstance().loadUsernameIntoTextView(userId, tv);
        else GPlusUtils.getInstance().loadUsernameIntoTextView(userId, tv);
    }

    public void getProfilePictureURL(final String user_id, final String account_type, final PlacesLoginUtils.PicSize size, final PlacesUtilCallback cbk) {
        if (account_type == null || account_type.equals("") || account_type.equals("fb"))
            FacebookUtils.getInstance().getFbProfilePictureURL(user_id, size, cbk);
        else if (account_type.equals("g+")) GPlusUtils.getInstance().getGPlusProfilePictureURL(user_id, size, null, cbk);
    }

    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size) {
        if (loginType == LoginType.FACEBOOK)
            FacebookUtils.getInstance().loadProfilePicIntoImageView(user_id, imageView, size);
        else GPlusUtils.getInstance().loadProfilePicIntoImageView(user_id, imageView, size, null);
    }

    public boolean isUserNameCached(String friendId) {
        return this.userIdMap.containsKey(friendId);
    }

    public enum PicSize {
        SMALL, LARGE;

        public String toString() {
            if (this == SMALL) {
                return SMALL_PIC_SIZE+"";
            }
            return LARGE_PIC_SIZE+"";
        }
    }

    public enum LoginType {FACEBOOK, GPLUS}
}