package com.gcw.sapienza.places.models;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;

/**
 * Class that encapsulate the Post table on Parse.com
 */
@ParseClassName("Posts")
public class Flag extends ParseObject {

    public static final long MAX_FILE_SIZE_BYTES = 10000000;

    public static final String AUDIO_KEY = "audio";
    public static final String VIDEO_KEY = "video";
    public static final String PICTURE_KEY = "picture";
    public static final String PHONE_MEDIA_KEY = "phone_media";
    public static final String WEATHER_KEY = "weather";
    public static final String FB_ID_KEY = "fbId";
    public static final String FB_NAME_KEY = "fbName";
    public static final String CATEGORY_KEY = "category";
    public static final String TEXT_KEY = "text";
    public static final String LOCATION_KEY = "location";
    public static final String IN_PLACE_KEY = "inPlace";
    public static final String THUMBNAIL_KEY = "thumbnail";
    public static final String WOW_COUNT_KEY = "wowCount";
    public static final String LOL_COUNT_KEY = "lolCount";
    public static final String BOO_COUNT_KEY = "booCount";
    public static final String OWNER_KEY = "owner";
    public static final String PASSWORD_KEY = "password";
    public static final String ACCOUNT_TYPE_KEY = "accountType";
    public static final String COMMENTS_COUNT_KEY = "num_comments";


    public void setOwner(ParseUser owner) {
        this.put(OWNER_KEY, owner);
    }

    public ParseUser getOwner() {
        return (ParseUser) this.get(OWNER_KEY);
    }

    public String getFlagId() {
        return this.getObjectId();
    }

    public String getText() {
        return (String) this.get(TEXT_KEY);
    }

    public void setText(String text) {
        this.put(TEXT_KEY, text);
    }

    public String getCategory() {
        return (String) this.get(CATEGORY_KEY);
    }

    public void setCategory(String category) {
        this.put(CATEGORY_KEY, category);
    }

    public String getFbId() {
        return (String) this.get(FB_ID_KEY);
    }

    public void setFbId(String fbId) {
        this.put(FB_ID_KEY, fbId);
    }

    public ParseGeoPoint getLocation() {
        return (ParseGeoPoint) this.get(LOCATION_KEY);
    }

    public void setLocation(ParseGeoPoint location) {
        this.put(LOCATION_KEY, location);
    }

    public Date getDate() {
        return this.getCreatedAt();
    }

    public ParseFile getPic() {
        return (ParseFile) this.get(PICTURE_KEY);
    }

    public String getWeather() {
        return (String) this.get(WEATHER_KEY);
    }

    public void setWeather(String weather) {
        this.put(WEATHER_KEY, weather);
    }

    public ParseFile getAudio() {
        return (ParseFile) this.get(AUDIO_KEY);
    }

    public ParseFile getVideo() {
        return (ParseFile) this.get(VIDEO_KEY);
    }

    public ParseFile getThumbnail() {
        return (ParseFile) this.get(THUMBNAIL_KEY);
    }

    public String getFbName() {
        return (String) this.get(FB_NAME_KEY);
    }

    public void setFbName(String name) {
        this.put(FB_NAME_KEY, name);
    }

    public boolean getInPlace() {
        return this.getBoolean(IN_PLACE_KEY);
    }

    public void setInPlace(boolean inPlace) {
        this.put(IN_PLACE_KEY, inPlace);
    }

    public int getWowCount() {
        return this.getInt(WOW_COUNT_KEY);
    }

    public int getNumberOfComments(){
        return this.getInt(COMMENTS_COUNT_KEY);
    }

    public int getLolCount() {
        return this.getInt(LOL_COUNT_KEY);
    }

    public int getBooCount() {
        return this.getInt(BOO_COUNT_KEY);
    }

    public String getPassword() {
        return this.getString(PASSWORD_KEY);
    }

    public String getAccountType() { return this.getString(ACCOUNT_TYPE_KEY); }

    public void setAccountType(String accountType) { this.put(ACCOUNT_TYPE_KEY, accountType); }

    public void setPassword(String psw) {
        this.put(PASSWORD_KEY, psw);
    }

    public void setThumbnailFile(ParseFile pic) {
        this.put(THUMBNAIL_KEY, pic);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPictureFile(ParseFile pic) {
        this.put(PICTURE_KEY, pic);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setAudioFile(ParseFile audio) {
        this.put(AUDIO_KEY, audio);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setVideoFile(ParseFile video) {
        this.put(VIDEO_KEY, video);
    }

    public void incrementWowCount() {
        this.increment(WOW_COUNT_KEY);
    }

    public void decrementWowCount() {
        this.increment(WOW_COUNT_KEY, -1);
    }

    public void incrementLolCount() {
        this.increment(LOL_COUNT_KEY);
    }

    public void decrementLolCount() {
        this.increment(LOL_COUNT_KEY, -1);
    }

    public void incrementBooCount() {
        this.increment(BOO_COUNT_KEY);
    }

    public void decrementBooCount() {
        this.increment(BOO_COUNT_KEY, -1);
    }
}
