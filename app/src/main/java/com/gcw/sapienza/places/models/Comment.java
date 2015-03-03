package com.gcw.sapienza.places.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

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

    public String getUserId() {
        return this.getString(USER_ID_KEY);
    }

    public String getFlagId() {
        return this.getString(FLAG_ID_KEY);
    }

    public String getCommentText() {
        return this.getString(TEXT_KEY);
    }

    public String getUsername() {
        return this.getString(USERNAME_KEY);
    }

    public Date getTimestamp() {
        return this.getCreatedAt();
    }

    public void setUserId(String userId) {
        this.put(USER_ID_KEY, userId);
    }

    public void setFlagId(String flagId) {
        this.put(FLAG_ID_KEY, flagId);
    }

    public void setCommentText(String text) {
        this.put(TEXT_KEY, text);
    }

    public void setUsername(String username) {
        this.put(USERNAME_KEY, username);
    }
}
