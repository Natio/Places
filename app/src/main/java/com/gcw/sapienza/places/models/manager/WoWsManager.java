package com.gcw.sapienza.places.models.manager;

import android.support.annotation.NonNull;

import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.WoWObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Subclass of ModelManager to handle query to comments
 */

/**
 * Created by snowblack on 4/30/15.
 */
public class WoWsManager extends ModelManager<WoWObject> {


    /**
     *
     * @param user the user that has commented
     * @param includeFlags if true all the associated flags will be loaded
     * @return al the wows of a given user
     */
    public Promise<WoWObject> wowsOfUser(@NonNull ParseUser user,boolean includeFlags){
        ParseQuery<WoWObject> q = this.instantiateEmptyQuery();
        q.whereEqualTo(WoWObject.USER_KEY, user);
        if(includeFlags){
            q.include(Comment.FLAG_KEY);
        }
        return new Promise<>(q);
    }

    /**
     *
     * @param f the flag
     * @return all wows for a flag, wows are not sorted
     */
    public Promise<WoWObject> wowsForFlag(@NonNull Flag f){
        ParseQuery<WoWObject> q = this.instantiateEmptyQuery();
        q.whereEqualTo(WoWObject.FLAG_KEY, f);
        return new Promise<>(q);
    }


    @Override
    protected Class<WoWObject> parseClass() {
        return WoWObject.class;
    }
}
