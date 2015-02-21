package com.gcw.sapienza.places.model;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject{

    public static final long MAX_FILE_SIZE_BYTES = 10000000;

    public static final String AUDIO_KEY = "audio";
    public static final String VIDEO_KEY = "video";
    public static final String PICTURE_KEY= "picture";
    public static final String PHONE_MEDIA_KEY = "phone_media";
    public static final String WEATHER_KEY = "weather";
    public static final String FB_ID_KEY = "fbId";
    public static final String FB_NAME_KEY = "fbName";
    public static final String CATEGORY_KEY = "category";
    public static final String TEXT_KEY = "text";
    public static final String LOCATION_KEY = "location";
    public static final String IN_PLACE_KEY = "inPlace";
    public static final String THUMBNAIL_KEY = "thumbnail";
    public static final String WOW_KEY = "wowIds";

    /**
     *
     * @return the text content of the post
     */

    public String getFlagId() { return this.getObjectId(); }

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

    public ParseFile getThumbnail() { return (ParseFile)this.get(THUMBNAIL_KEY);  }

    public String getFbName(){
        return (String)this.get(FB_NAME_KEY);
    }

    public boolean getInPlace() { return this.getBoolean(IN_PLACE_KEY); }

    public ArrayList<String> getWowIds() { return (ArrayList<String>)this.get(WOW_KEY); }


    public void setThumbnailFile(ParseFile pic){
        this.put(THUMBNAIL_KEY, pic);
    }
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

    public void setFbName(String name) { this.put(FB_NAME_KEY, name); }

    public void setInPlace(boolean inPlace) { this.put(IN_PLACE_KEY, inPlace); }

    public void addWowId(String wowId) { this.add(WOW_KEY, wowId); }

    public void deleteWowId(String wowId)
    {
        ArrayList<String> idToDelete = new ArrayList();
        idToDelete.add(wowId);

        this.removeAll(WOW_KEY, idToDelete);
    }
}
