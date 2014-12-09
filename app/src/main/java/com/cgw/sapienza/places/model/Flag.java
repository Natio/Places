package com.cgw.sapienza.places.model;

import com.parse.ParseClassName;
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


}
