package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.Date;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject{

    public static final String AUDIO_KEY = "audio";
    public static final String VIDEO_KEY = "video";
    public static final String PICTURE_KEY= "picture";
    public static final String WEATHER_KEY = "weather";
    public static final String FB_ID_KEY = "fbId";
    public static final String CATEGORY_KEY = "category";
    public static final String TEXT_KEY = "text";
    public static final String LOCATION_KEY = "location";

    /**
     *
     * @return the text content of the post
     */

    public String getText(){ return (String)this.get(TEXT_KEY); }

    public String getCategory(){ return(String)this.get(CATEGORY_KEY); }

    public String getFbId()
    {
        return (String)this.get(FB_ID_KEY);
    }

    public ParseGeoPoint getLocation(){
        return (ParseGeoPoint)this.get(LOCATION_KEY);
    }

    public Date getDate() { return this.getCreatedAt(); }

    public ParseFile getPic() { return (ParseFile)this.get(PICTURE_KEY); }

    public String getWeather() { return (String)this.get(WEATHER_KEY);  }

    public ParseFile getAudio() { return (ParseFile)this.get(AUDIO_KEY);  }

    public ParseFile getVideo() { return (ParseFile)this.get(VIDEO_KEY);  }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPictureFile(ParseFile pic){
        this.put(PICTURE_KEY, pic);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setAudioFile(ParseFile audio){
        this.put(AUDIO_KEY, audio);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setVideoFile(ParseFile video){
        this.put(VIDEO_KEY, video);
    }

    public void setCategory(String category){
        this.put(CATEGORY_KEY, category);
    }

    public void setWeather(String weather){
        this.put(WEATHER_KEY, weather);
    }

    public void setFbId(String fbId){
        this.put(FB_ID_KEY, fbId);
    }

    public void setLocation(ParseGeoPoint location){
        this.put(LOCATION_KEY, location);
    }

    public void setText(String text){
        this.put(TEXT_KEY, text);
    }

}
