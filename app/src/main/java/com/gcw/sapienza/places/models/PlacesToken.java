package com.gcw.sapienza.places.models;

import com.parse.ParseObject;

/**
 * Created by mic_head on 03/03/15.
 */
public class PlacesToken extends ParseObject {

    private static final String ACCOUNT_ID_KEY = "accountId";
    private static final String USER_KEY = "user";

    public String getAccountId() {
        return this.getString(ACCOUNT_ID_KEY);
    }

    public PlacesUser getUser() {
        return (PlacesUser) this.get("user");
    }

    public void setAccountId(String accountId) {
        this.put(ACCOUNT_ID_KEY, accountId);
    }

    public void setUser(PlacesUser user) {
        this.put(USER_KEY, user);
    }
}
