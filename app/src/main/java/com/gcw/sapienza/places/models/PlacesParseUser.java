package com.gcw.sapienza.places.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Date;

/**
 * Created by snowblack on 3/1/15.
 */
public class PlacesParseUser extends com.parse.ParseUser {

    public static final String FACEBOOK_ID_KEY = "fbId";

    public String getFacebookId() { return this.getString(FACEBOOK_ID_KEY); }

}
