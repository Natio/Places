package com.gcw.sapienza.places.models;

import com.parse.ParseUser;

/**
 * Created by mic_head on 01/03/15.
 */
public class PlacesUser extends ParseUser {

    private final static String TAG = "PlacesUser";

    public static final String NAME_KEY = "name";
    public static final String FACEBOOK_ID_KEY = "fbId";

    public String getName() { return this.getString(NAME_KEY); }

    public String getFbId() { return this.getString(FACEBOOK_ID_KEY); }

    public void setName(String name) { this.put(NAME_KEY, name); }

    public void setFbId(String fbId) { this.put(FACEBOOK_ID_KEY, fbId); }
}
