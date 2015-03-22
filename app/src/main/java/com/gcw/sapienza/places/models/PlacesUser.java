package com.gcw.sapienza.places.models;

import com.parse.ParseUser;

import java.io.Serializable;

/**
 * Created by mic_head on 01/03/15.
 */
public class PlacesUser extends ParseUser implements Serializable {

    public PlacesUser(){
    }

    private static final String TAG = "PlacesUser";

    public static final String NAME_KEY = "name";
    public static final String FACEBOOK_ID_KEY = "fbId";
    private static final String LOGIN_TYPE_KEY = "accountType";

    public String getName() {
        return this.getString(NAME_KEY);
    }

    public void setName(String name) {
        this.put(NAME_KEY, name);
    }

    public String getFbId() {
        return this.getString(FACEBOOK_ID_KEY);
    }

    public void setFbId(String fbId) {
        this.put(FACEBOOK_ID_KEY, fbId);
    }

    public String getAccountType() { return this.getString(LOGIN_TYPE_KEY); }

    public void setAccountType(String accountType) { this.put(LOGIN_TYPE_KEY, accountType); }
}
