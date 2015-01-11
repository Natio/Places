package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject{

    /**
     *
     * @return the text content of the post
     */

    public String getObjectId(){ return (String)this.get("objectId"); };

    public String getText(){ return (String)this.get("text"); }

    public String getCategory(){ return(String)this.get("category"); }

    public String getFbId()
    {
        return (String)this.get("fbId");
    }

    public ParseGeoPoint getLocation(){
        return (ParseGeoPoint)this.get("location");
    }

    public Date getDate() { return this.getCreatedAt(); }

    public ParseFile getPic() { return (ParseFile)this.get("picture"); }

    public String getWeather() { return (String)this.get("weather");  }

    public ArrayList<String> getReports() { return (ArrayList<String>)this.get("reports");  }

    public ParseFile getAudio() { return (ParseFile)this.get("audio");  }

}
