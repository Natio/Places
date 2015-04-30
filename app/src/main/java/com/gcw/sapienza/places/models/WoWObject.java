package com.gcw.sapienza.places.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by mic_head on 22/02/15.
 */
@ParseClassName("Wow_Lol_Boo")
public class WoWObject extends ParseObject {

    public static final String FACEBOOK_ID_KEY = "fbId";
    public static final String FLAG_ID_KEY = "flagId";
    public static final String FLAG_KEY = "flag";
    public static final String WOW_KEY = "boolWow";
    public static final String LOL_KEY = "boolLol";
    public static final String BOO_KEY = "boolBoo";
    public static final String USER_KEY = "user";

    public void setUser(ParseUser user) {
        this.put(USER_KEY, user);
    }

    public ParseUser getuser() {
        return (ParseUser) this.get(USER_KEY);
    }

    public String getFacebookId() {
        return (String) this.get(FACEBOOK_ID_KEY);
    }

    public void setFacebookId(String fbId) {
        this.put(FACEBOOK_ID_KEY, fbId);
    }

    public String getFlagId() {
        return (String) this.get(FLAG_ID_KEY);
    }

    public void setFlagId(String flagId) {
        this.put(FLAG_ID_KEY, flagId);
    }

    public Flag getFlag() {
        return (Flag) this.get(FLAG_KEY);
    }

    public void setFlag(Flag flag) {
        this.put(FLAG_KEY, flag);
    }

    public boolean getWowBoolean() {
        return (boolean) this.get(WOW_KEY);
    }

    public void setWowBoolean(boolean wowBool) {
        this.put(WOW_KEY, wowBool);
    }

    public boolean getLolBoolean() {
        return (boolean) this.get(LOL_KEY);
    }

    public void setLolBoolean(boolean lolBool) {
        this.put(LOL_KEY, lolBool);
    }

    public boolean getBooBoolean() {
        return (boolean) this.get(BOO_KEY);
    }

    public void setBooBoolean(boolean booBool) {
        this.put(BOO_KEY, booBool);
    }

}
