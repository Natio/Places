package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.gcw.sapienza.places.BuildConfig;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.WeatherHttpClient;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


/**
 * This class allows to asynchronously upload a Flag and to have completion callbacks
 * <p/>
 * <p/>
 * Usage:
 * <p/>
 * File video;
 * File audio;
 * File picture;
 * <p/>
 * FlagUploader up = new FlagUploader();
 * up.setVideoFile(video);
 * up.setAudioFile(audio);
 * up.setPictureFile(picture);
 * <p/>
 * uploader.upload(new FlagUploaderCallbacks{...});
 * <p/>
 * <p/>
 * How It Works
 * <p/>
 * When upload() is called FlagUploader.loadFileLoop() is automatically invoked.
 * FlagUploader.loadFileLoop() is the responsible of ParseFile creation and loading.
 * <p/>
 * 1) loadFileLoop() checks that this.files is not empty
 * 2) loadFileLoop() removes a File from this.files (that is the Files to upload storage)and schedules a FileLoaderTask that loads the file
 * in memory and creates a ParseFile.
 * 3) When the ParseFile is configured FileLoaderTask calls FlagUploader onParseFileInMemoryLoadDone(ParseFile parse_file)
 * 4) onParseFileInMemoryLoadDone starts the ParseFile upload; when the upload is finished control returns to 1.
 * <p/>
 * 5) if this.files is empty loadFlag() will be called and will upload the flag to Parse.com
 * 6) in case of success onFinish() will be called otherwise onError()
 * <p/>
 * <p/>
 * Created by paolo on 12/01/15.
 */
public class FlagUploader {
    private static final String TAG = "FlagUploader";

    private static final String AUDIO_KEY = Flag.AUDIO_KEY;
    private static final String VIDEO_KEY = Flag.VIDEO_KEY;
    private static final String PICTURE_KEY = Flag.PICTURE_KEY;
    private static final String PHONE_MEDIA_KEY = Flag.PHONE_MEDIA_KEY;

    private final Flag flag;
    private final Context context;
    private File thumbnail;
    private HashMap<String, File> files;
    private HashMap<String, File> usedFiled;
    private boolean isUploading;
    //  private boolean deleteFilesOnFinish = false;
    private FlagUploaderCallbacks callbacks;
    private String currentFileKey = null;

    /**
     * Creates the instance
     *
     * @param f   the flag to upload
     * @param ctx a context
     * @throws IllegalArgumentException if cbk is null
     */
    public FlagUploader(Flag f, Context ctx) {
        this.flag = f;
        this.isUploading = false;
        this.files = new HashMap<>(3);
        this.usedFiled = new HashMap<>(3);
        this.context = ctx;
        this.thumbnail = null;
        this.flag.setOwner(ParseUser.getCurrentUser());

    }

    /**
     * @return true if method upload has been already called, false otherwise
     */
    @SuppressWarnings("UnusedDeclaration")
    public boolean isUploading() {
        return this.isUploading;
    }

    /**
     * Sets the ParseFile representing a video
     *
     * @param video             file to upload as a video
     * @param generateThumbnail if true a thumbnail is added to the flag
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     */
    public void setVideoFile(File video, boolean generateThumbnail) {
        if (this.isUploading) {
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        if (generateThumbnail) {
            this.thumbnail = BitmapUtils.createTumbnailForVideo(video);
        }
        this.files.put(VIDEO_KEY, video);
    }

    /**
     * Sets the ParseFile representing a audio rec
     *
     * @param audio file to upload as a audio rec
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     */
    public void setAudioFile(File audio) {
        if (this.isUploading) {
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(AUDIO_KEY, audio);
    }

    /**
     * Sets the ParseFile representing a picture
     *
     * @param picture           file to upload as a picture
     * @param generateThumbnail if true a thumbnail is added to the flag
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     */
    public void setPictureFile(File picture, boolean generateThumbnail) {
        if (this.isUploading) {
            throw new IllegalStateException("Cannot set a file while uploading");
        }

        if (generateThumbnail) {
            this.thumbnail = BitmapUtils.createThumbnailForImageRespectingProportions(picture);
        }

        this.files.put(PICTURE_KEY, picture);
    }

    /**
     * Sets the ParseFile representing a picture
     *
     * @param media file to upload as a phone media
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setPhoneMediaFile(File media) {
        if (this.isUploading) {
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(PHONE_MEDIA_KEY, media);
    }

    /**
     * Adds a thumbnail
     *
     * @param thumbnail file containing the thumbnail picture
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setThumbnail(File thumbnail) {
        if (this.isUploading) {
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.thumbnail = thumbnail;
    }

    /**
     * @param cbk callbacks MUST NOT be null
     */
    public void upload(FlagUploaderCallbacks cbk) {
        if (cbk == null) {
            throw new IllegalArgumentException("callbacks parameter must not be null");
        }

        this.callbacks = cbk;
        this.isUploading = true;

        new WeatherTask().execute(PlacesApplication.getInstance().getLocation());


    }


    /**
     * When this method is called it starts to upload a file asynchronously.
     * When a file is uploaded this method will be called again and a new upload will start until
     * there will be nothing to upload. If there is nothing to upload the Flag will be saved
     */
    private void loadFileLoop() {
        if (BuildConfig.DEBUG && Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new RuntimeException("Something went wrong with threads");
        }

        if (this.thumbnail != null) {
            new ThumbFileLoaderTask().execute(this.thumbnail);
            this.thumbnail = null;
            return;
        }

        if (this.files.isEmpty()) {
            this.loadFlag();
            return;
        }

        String key = this.getNextKeyFromFilesMap();
        this.currentFileKey = key;
        File current_file = this.files.remove(key);
        this.usedFiled.put(key, current_file);

        //horrible hack to support image scaling
        //in order to remove scaling from main thread
        if (this.currentFileKey.equals(PICTURE_KEY)) {
            new PictureFileLoaderTask().execute(current_file);
        } else {
            new FileLoaderTask().execute(current_file);
        }
    }

    /**
     * Called when an error occurs. If this method is called the upload is stopped and no data will be created on parse.com
     *
     * @param e error description
     */

    private void onError(Exception e) {
        this.callbacks.onError(e);
    }


    /**
     * Called when a ParseFile is successfully loaded in memory
     *
     * @param parse_file parse file to upload
     */
    private void onParseFileInMemoryLoadDone(ParseFile parse_file) {
        if (parse_file == null) {
            this.onError(new Exception("Error loading file. Flag cannot be placed"));
            return;
        }
        this.flag.put(this.currentFileKey, parse_file);
        final String message_to_user = this.getUserMessageForKey(this.currentFileKey);
        this.currentFileKey = null;

        FlagUploader.this.callbacks.onPercentage(0, message_to_user);
        parse_file.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    FlagUploader.this.onError(e);
                } else {
                    FlagUploader.this.loadFileLoop();
                }
            }
        }, new ProgressCallback() {
            @Override
            public void done(Integer integer) {
                FlagUploader.this.callbacks.onPercentage(integer, message_to_user);
            }
        });
    }

    private void onThumbInMemoryLoadDone(ParseFile parseFile) {
        this.flag.setThumbnailFile(parseFile);

        parseFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    FlagUploader.this.onError(e);
                } else {
                    FlagUploader.this.loadFileLoop();
                }
            }
        }, new ProgressCallback() {
            @Override
            public void done(Integer integer) {
                FlagUploader.this.callbacks.onPercentage(integer, FlagUploader.this.getUserMessageForKey(PICTURE_KEY));
            }
        });
    }


    /**
     * When all files are uploaded ti uploads the flag
     */
    private void loadFlag() {
        // FlagUploader.this.callbacks.onPercentage(100, "Loading last bits :)");
        System.gc();
        this.flag.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    FlagUploader.this.onError(e);
                } else {
                    FlagUploader.this.onFinish();
                }
            }
        });

    }

    /**
     *
     * @param weather string representing the weather
     */
    private void onWeatherLoaded(String weather){
        if(weather != null){
            this.flag.setWeather(weather);
        }

        FacebookUtils.getInstance().getFacebookUsernameFromID(PlacesLoginUtils.getInstance().getCurrentUserId(), new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                FlagUploader.this.flag.setFbName(result);
                FlagUploader.this.loadFileLoop();
            }
        });

    }

    /**
     * Called when everything is uploaded. If #deleteOnFinish is true all files will be deleted
     */
    private void onFinish() {
        FlagUploader.this.callbacks.onSuccess();
        /*if(this.deleteFilesOnFinish){
            for(String key : this.usedFiled.keySet()){
                File f = this.usedFiled.get(key);
                Log.d(TAG, "Deleted file: "+f.getName()+ " ? "+ f.delete());
            }
        }*/
        File f = this.usedFiled.get(AUDIO_KEY);
        if (f != null) {
            Log.d(TAG, "Deleted file: " + f.getName() + " ? " + f.delete());
        }
        this.usedFiled.clear();
    }

    /**
     * @param key file key
     * @return a string representing the message to the user
     */
    private String getUserMessageForKey(String key) {
        switch (key) {
            case AUDIO_KEY:
                return this.context.getString(R.string.upload_audio_progress);
            case PICTURE_KEY:
                return this.context.getString(R.string.upload_picture_progress);
            case VIDEO_KEY:
                return this.context.getString(R.string.upload_video_progress);
            case PHONE_MEDIA_KEY:
                return this.context.getString(R.string.upload_phone_media_progress);
        }

        return null;
    }

    /**
     * @return the correct key for the current upload round
     */
    private String getNextKeyFromFilesMap() {
        if (this.files.containsKey(AUDIO_KEY)) {
            return AUDIO_KEY;
        } else if (this.files.containsKey(PICTURE_KEY)) {
            return PICTURE_KEY;
        } else if (this.files.containsKey(VIDEO_KEY)) {
            return VIDEO_KEY;
        } else if (this.files.containsKey(PHONE_MEDIA_KEY)) {
            return PHONE_MEDIA_KEY;
        }
        return null;
    }


    public interface FlagUploaderCallbacks {
        /**
         * Progress of current upload
         *
         * @param percentage   value between 0 and 100
         * @param text_to_show text to display to the user
         */
        void onPercentage(int percentage, String text_to_show);

        /**
         * Called when an error occurs. #onSucces() wont be called in case of error
         *
         * @param e error
         */
        void onError(Exception e);

        /**
         * Called when everything is uploaded
         */
        void onSuccess();
    }


    private class FileLoaderTask extends AsyncTask<File, Integer, ParseFile> {
        @Override
        protected ParseFile doInBackground(File... params) {
            if (params.length == 0) {
                return null;
            }

            File file = params[0];

            try {
                return new ParseFile(this.getNameFromFile(file), this.loadFileInMemory(file));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected String getNameFromFile(File f) {
            return f.getName();
        }


        private byte[] loadFileInMemory(File f) throws IOException {
            FileInputStream is = new FileInputStream(f);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            byte[] buff = new byte[Utils.CHUNK_SIZE];
            int i;
            while ((i = is.read(buff, 0, buff.length)) > 0) {
                outStream.write(buff, 0, i);
            }
            return outStream.toByteArray();
        }

        @Override
        protected void onPostExecute(ParseFile file) {
            FlagUploader.this.onParseFileInMemoryLoadDone(file);
        }
    }

    private class PictureFileLoaderTask extends FileLoaderTask {
        @Override
        protected ParseFile doInBackground(File... params) {
            if (params.length == 0) {
                return null;
            }
            File file = params[0];
            File scaledImage = BitmapUtils.scaleImageToMaxSupportedSize(file);
            params[0] = scaledImage;
            return super.doInBackground(params);
        }

        @Override
        protected String getNameFromFile(File f) {
            return "image." + Utils.getExtensionFromFile(f);
        }

    }

    private class ThumbFileLoaderTask extends FileLoaderTask {

        @Override
        protected String getNameFromFile(File f) {
            return "thumb." + Utils.getExtensionFromFile(f);
        }

        @Override
        protected void onPostExecute(ParseFile file) {
            FlagUploader.this.onThumbInMemoryLoadDone(file);
        }
    }

    private class WeatherTask extends AsyncTask<Location, Integer, String>{


        private String getLocalityFromCoorinate(Location current){
            Geocoder gcd = new Geocoder(PlacesApplication.getPlacesAppContext(), Locale.getDefault());
            try {

                List<Address> addresses = gcd.getFromLocation(current.getLatitude(), current.getLongitude(), 1);
                if (addresses.size() > 0) {
                    Log.d(TAG, "Locality: " + addresses.get(0).getLocality());
                    String locality = addresses.get(0).getLocality();
                    String cc = addresses.get(0).getCountryCode();
                    return  locality + ',' + cc;
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "No locality found! Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected String doInBackground(Location... params) {


            String locality = this.getLocalityFromCoorinate(params[0]);
            if (locality == null){
                return null;
            }

            String data = ((new WeatherHttpClient()).getWeatherData(locality));
            try {
                Log.d("Weather", data);
                JSONObject info = new JSONObject(data);
                JSONObject mainObj = info.getJSONObject("main");

                float temp = (float) mainObj.getDouble("temp") - 273.15f;
                int round_temp = Math.round(temp);
                Log.d("Weather", "Temperatura: " + round_temp);

                JSONArray weatherObj = info.getJSONArray("weather");
                String cond = weatherObj.getJSONObject(0).getString("main");
                Log.d("Weather", "Condizioni: " + cond);
                return round_temp + "°C, " + cond;

            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            FlagUploader.this.onWeatherLoaded(s);
            super.onPostExecute(s);
        }
    }

}
