package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import java.util.ArrayList;

/**
 * Created by mic_head on 22/02/15.
 */
@ParseClassName("Wow_Lol_Boo")
public class CustomUser extends ParseObject {

    public static final String FACEBOOK_ID_KEY = "fbId";
    public static final String WOW_KEY = "wows";
    public static final String LOL_KEY = "lols";
    public static final String BOO_KEY = "boos";


    public String getUserId() { return this.getObjectId(); }

    public String getFacebookId() { return (String)this.get(FACEBOOK_ID_KEY); }

    public ArrayList<String> getWows() { return (ArrayList<String>)this.get(WOW_KEY); }

    public ArrayList<String> getLols() { return (ArrayList<String>)this.get(LOL_KEY); }

    public ArrayList<String> getBoos() { return (ArrayList<String>)this.get(BOO_KEY); }

    public void setFacebookId(String fbId) { this.put(FACEBOOK_ID_KEY, fbId); }

    public void addWow(String wowId) { this.add(WOW_KEY, wowId); }

    public void deleteWow(String wowId)
    {
        ArrayList<String> idToDelete = new ArrayList();
        idToDelete.add(wowId);

        this.removeAll(WOW_KEY, idToDelete);
    }

    public void addLol(String lolId) { this.add(LOL_KEY, lolId); }

    public void deleteLol(String lolId)
    {
        ArrayList<String> idToDelete = new ArrayList();
        idToDelete.add(lolId);

        this.removeAll(LOL_KEY, idToDelete);
    }

    public void addBoo(String booId) { this.add(BOO_KEY, booId); }

    public void deleteBoo(String booId)
    {
        ArrayList<String> idToDelete = new ArrayList();
        idToDelete.add(booId);

        this.removeAll(BOO_KEY, idToDelete);
    }
}
