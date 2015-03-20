package com.gcw.sapienza.places.models;

import com.gcw.sapienza.places.PlacesApplication;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;

/**
 * Created by mic_head on 25/02/15.
 */
@ParseClassName("Comments")
public class Comment extends ParseObject {

    public static final String USER_ID_KEY = "userId";
    public static final String FLAG_ID_KEY = "flagId";
    public static final String TEXT_KEY = "text";
    public static final String USERNAME_KEY = "username";
    public static final String FLAG_KEY = "flag";
    public static final String FLAG_OWNER_KEY = "flagOwner";

    public String getUserId() {
        return this.getString(USER_ID_KEY);
    }

    public void setUserId(String userId) {
        this.put(USER_ID_KEY, userId);
    }

    public String getFlagId() {
        return this.getString(FLAG_ID_KEY);
    }

    public void setFlagId(String flagId) {
        this.put(FLAG_ID_KEY, flagId);
    }

    public Flag getFlag() {
        return (Flag)this.getParseObject(FLAG_KEY);
    }

    public void setFlag(Flag flag) { this.put(FLAG_KEY, flag); }

    public PlacesUser getFlagOwner() { return (PlacesUser)this.getParseObject(FLAG_OWNER_KEY); }

    public void setFlagOwner(ParseUser flag) { this.put(FLAG_KEY, flag); }

    public String getCommentText() {
        return this.getString(TEXT_KEY);
    }

    public void setCommentText(String text) {
        this.put(TEXT_KEY, text);
    }

    public String getUsername() {
        return this.getString(USERNAME_KEY);
    }

    public void setUsername(String username) {
        this.put(USERNAME_KEY, username);
    }

    public Date getTimestamp() {
        return this.getCreatedAt();
    }
}
