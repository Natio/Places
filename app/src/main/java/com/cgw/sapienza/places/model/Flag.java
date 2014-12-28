package com.cgw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject{

    /**
     *
     * @return the text content of the post
     */

    public String getText(){

        return (String)this.get("text");
    }

    public String getFbId()
    {
        return (String)this.get("fbId");
    }

    public ParseGeoPoint getLocation(){
        return (ParseGeoPoint)this.get("location");
    }


}
