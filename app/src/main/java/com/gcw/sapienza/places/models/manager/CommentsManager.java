package com.gcw.sapienza.places.models.manager;

import android.support.annotation.NonNull;

import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.Flag;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Subclass of ModelManager to handle query to comments
 */
public class CommentsManager extends ModelManager<Comment> {


    /**
     *
     * @param user the user that has commented
     * @param includeFlags if true all the associated flags will be loaded
     * @return al the comments of a given user
     */
    public Promise<Comment> commentOfUser(@NonNull ParseUser user,boolean includeFlags){
        ParseQuery<Comment> q = this.instanciateEmptyQuery();
        q.whereEqualTo(Comment.COMMENT_OWNER_KEY, user);
        if(includeFlags){
            q.include(Comment.FLAG_KEY);
        }
        return new Promise<>(q);
    }

    /**
     *
     * @param f the flag
     * @return all comments for a flag, comments are not sorted
     */
    public Promise<Comment> commentsForFlag(@NonNull Flag f){
        ParseQuery<Comment> q = this.instanciateEmptyQuery();
        q.whereEqualTo(Comment.FLAG_KEY, f);
        return new Promise<>(q);
    }


    @Override
    protected Class<Comment> parseClass() {
        return Comment.class;
    }
}
