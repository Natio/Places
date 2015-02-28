package com.gcw.sapienza.places.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.activities.PlacesLoginActivity;
import com.parse.ui.ParseLoginActivity;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mic_head on 27/02/15.
 */
public class PlacesLoginUtils {

    private static final String TAG = "PlacesLoginUtils";

    protected static final String LARGE_PIC_SIZE = "200";
    protected static final String SMALL_PIC_SIZE = "120";

    public enum PicSize {
        SMALL, LARGE;

        public String toString() {
            if (this == SMALL) {
                return SMALL_PIC_SIZE;
            }
            return LARGE_PIC_SIZE;
        }
    }

    private String userId;

    public enum LoginType{ FACEBOOK, GPLUS }

    public static LoginType loginType;

    private final ArrayList<String> friends = new ArrayList<>();
    private final HashMap<String, String> userIdMap = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapSmall = new HashMap<>();
    private final HashMap<String, String> userProfilePicMapLarge = new HashMap<>();

    private static final PlacesLoginUtils shared_instance = new PlacesLoginUtils();


    private PlacesLoginUtils() {}

    /**
     * This is a singleton class. This method returns the ONLY instance
     *
     * @return Singleton instance
     */
    public static PlacesLoginUtils getInstance() {
        return PlacesLoginUtils.shared_instance;
    }

    public static void downloadUserInfo(Context context)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.downloadFacebookInfo(context);
        else if(loginType == LoginType.GPLUS) GPlusUtils.downloadGPlusInfo(GPlusUtils.getInstance().getGoogleApiClient(), context);
    }

    public String getCurrentUserId()
    {
        return this.userId;
    }

    public ArrayList<String> getFriends()
    {
        return this.friends;
    }

    public HashMap<String, String> getUserIdMap()
    {
        return this.userIdMap;
    }

    public HashMap<String, String> getUserProfilePicMapSmall()
    {
        return this.getUserProfilePicMapSmall();
    }

    public HashMap<String, String> getUserProfilePicMapLarge()
    {
        return this.getUserProfilePicMapLarge();
    }

    public void setCurrentUserId(String userId)
    {
        this.userId = userId;
    }

    public void addFriend(String friend)
    {
        this.friends.add(friend);
    }

    public void setFriends(ArrayList<String> friends)
    {
        this.friends.addAll(friends);
    }

    public void addEntryToUserIdMap(String key, String value)
    {
        this.userIdMap.put(key, value);
    }

    public void addEntryToSmallPicMap(String key, String value)
    {
        this.userProfilePicMapSmall.put(key, value);
    }

    public void addEntryToLargePicMap(String key, String value)
    {
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

    public void clearFriends()
    {
        this.friends.clear();
    }

    public void clearUserIdMap()
    {
        this.userIdMap.clear();
    }

    public void clearUserProfilePicMapSmall()
    {
        this.userProfilePicMapSmall.clear();
    }

    public void clearUserProfilePicMapLarge()
    {
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
    protected String getUserNameFromId(String id) {
        return this.userIdMap.get(id);
    }

    public String getProfilePictureSmall(String profile_id) {
        return this.userProfilePicMapSmall.get(profile_id);
    }

    public String getProfilePictureLarge(String profile_id) {
        return this.userProfilePicMapLarge.get(profile_id);
    }

    public boolean isSessionValid(Activity activity)
    {
        if(FacebookUtils.isFacebookSessionOpened())
        {
            loginType = LoginType.FACEBOOK;

            return true;
        }
        else if(GPlusUtils.getInstance().getGoogleApiClient() == null ||
                !GPlusUtils.getInstance().getGoogleApiClient().isConnected())
            startLoginActivity(activity);
        else if(GPlusUtils.getInstance().getGoogleApiClient().isConnected())
        {
            loginType = LoginType.GPLUS;

            return true;
        }

        return false;
    }

    /**
     * @param profile_id profile idenrifier
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

    /**
     *
     * @param activity the activity where to start the intent
     */
    public static void startLoginActivity(Activity activity)
    {
        PlacesLoginBuilder builder = new PlacesLoginBuilder(activity);

        // builder.setAppLogo(R.drawable.app_logo);
        Intent loginIntent = builder.build();
        loginIntent.setClass(activity, PlacesLoginActivity.class);
        // loginIntent.setClass(activity, ParseLoginActivity.class);

        activity.startActivityForResult(loginIntent, Utils.LOGIN_REQUEST_CODE);
    }

    public void loadUsernameIntoTextView(String fb_id, final TextView tv)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.getInstance().loadUsernameIntoTextView(fb_id, tv);
        else GPlusUtils.getInstance().loadUsernameIntoTextView(fb_id, tv);
    }

    public void getFbProfilePictureURL(final String user_id, final PlacesLoginUtils.PicSize size, final FacebookUtilCallback cbk)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.getInstance().getFbProfilePictureURL(user_id, size, cbk);
        else GPlusUtils.getInstance().getFbProfilePictureURL(user_id, size, cbk);
    }

    public void loadProfilePicIntoImageView(final String user_id, final ImageView imageView, final PlacesLoginUtils.PicSize size)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.getInstance().loadProfilePicIntoImageView(user_id, imageView, size);
        else GPlusUtils.getInstance().loadProfilePicIntoImageView(user_id, imageView, size);
    }
}