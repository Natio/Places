package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
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

    public Date getDate() { return (Date)this.getCreatedAt(); }

    public byte[] getPic() { return (byte[])this.get("pic"); }

    public String getWeather() { return (String)this.get("weather");  }

    public ArrayList<String> getReports() { return (ArrayList<String>)this.get("reports");  }

}
