package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.Date;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject{

    /**
     *
     * @return the text content of the post
     */

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

<<<<<<< HEAD
    /**
     *
     * @return Integer.MAX_VALUE if there is no temperature
     */
    public int getTemp() {
        Integer temp =(Integer) this.get("temp");
        if(temp == null){
            return Integer.MAX_VALUE;
        }
        return temp;
    }

=======
>>>>>>> 8786bf2bf9f6655c84ba91772771fad751df6c56
    public byte[] getPic() { return (byte[])this.get("pic"); }

    public String getWeather() { return (String)this.get("weather");  }
}
