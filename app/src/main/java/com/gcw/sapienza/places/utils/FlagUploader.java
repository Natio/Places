package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.gcw.sapienza.places.BuildConfig;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.util.HashMap;


/**
 * This class allows to asynchronously upload a Flag and to have completion callbacks
 *
 * Created by paolo on 12/01/15.
 */
public class FlagUploader {
    private static final String TAG = "FlagUploader";

    private static final String AUDIO_KEY = "audio";
    private static final String VIDEO_KEY = "video";
    private static final String PICTURE_KEY = "picture";

    private final Flag flag;
    private HashMap<String, ParseFile> files;
    private boolean isUploading;
    private FlagUploaderCallbacks callbacks;

    private final Context context;

    /**
     * Creates the instance
     * @param f the flag to upload
     * @param ctx a context
     * @throws IllegalArgumentException if cbk is null
     */
    public FlagUploader(Flag f, Context ctx){
        this.flag = f;
        this.isUploading = false;
        this.files = new HashMap<>();
        this.context = ctx;
    }


    /**
     *
     * @return true if method upload has been already called, false otherwise
     */
    public boolean isUploading(){
        return this.isUploading;
    }

    /**
     * Sets the ParseFile representing a video
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param video file to upload as a video
     */
    public void setVideoFile(ParseFile video){
        if(this.isUploading()){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(VIDEO_KEY, video);
    }

    /**
     * Sets the ParseFile representing a audio rec
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param audio file to upload as a audio rec
     */
    public void setAudioFile(ParseFile audio){
        if(this.isUploading()){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(AUDIO_KEY, audio);
    }

    /**
     * Sets the ParseFile representing a picture
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param picture file to upload as a picture
     */
    public void setPictureFile(ParseFile picture){
        if(this.isUploading()){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(PICTURE_KEY, picture);
    }


    /**
     *
     * @param cbk callbacks MUST NOT be null
     */
    public void upload( FlagUploaderCallbacks cbk){
        if(cbk == null){
            throw new IllegalArgumentException("callbacks parameter must not be null");
        }

        this.callbacks = cbk;
        this.isUploading = true;

        this.loadFileLoop();

    }


    /**
     * When this method is called it starts to upload a file asynchronously.
     * When a file is uploaded this method will be called again and a new upload will start until
     *  there will be nothing to upload. If there is nothing to upload the Flag will be saved
     */
    private void loadFileLoop(){
        Log.d(TAG, "CIAo");
        if(BuildConfig.DEBUG && Looper.getMainLooper().getThread() != Thread.currentThread()){
            throw new RuntimeException("Something went wrong with threads");
        }

        if(this.files.isEmpty()){
            this.loadFlag();
            return;
        }

        final String key = this.getNextKeyFromFilesMap();
        final String message_to_user = this.getUserMessageForKey(key);
        ParseFile current_file = this.files.remove(key);


        FlagUploader.this.callbacks.onPercentage(0, message_to_user);
        current_file.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null){
                    FlagUploader.this.callbacks.onError(e);
                }
                else{
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

    /**
     * When all files are uploaded ti uploads the flag
     */
    private void loadFlag(){
        FlagUploader.this.callbacks.onPercentage(100, "Loading last bits :)");
        this.flag.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null){
                    FlagUploader.this.callbacks.onError(e);
                }
                else{
                    FlagUploader.this.callbacks.onSuccess();
                }
            }
        });
    }

    /**
     *
     * @param key file key
     * @return a string representing the message to the user
     */
    private String getUserMessageForKey(String key){
        switch (key) {
            case AUDIO_KEY:
                return this.context.getString(R.string.upload_audio_progress);
            case PICTURE_KEY:
                return this.context.getString(R.string.upload_picture_progress);
            case VIDEO_KEY:
                return this.context.getString(R.string.upload_video_progress);
        }

        return null;
    }

    /**
     *
     * @return the correct key for the current upload round
     */
    private String getNextKeyFromFilesMap(){
        if(this.files.containsKey(AUDIO_KEY)){
            return AUDIO_KEY;
        }
        else if(this.files.containsKey(PICTURE_KEY)){
            return PICTURE_KEY;
        }
        else if(this.files.containsKey(VIDEO_KEY)){
            return VIDEO_KEY;
        }
        return null;
    }


    public interface FlagUploaderCallbacks{
        /**
         * Progress of current upload
         * @param percentage value between 0 and 100
         * @param text_to_show text to display to the user
         */
        void onPercentage(int percentage, String text_to_show);

        /**
         * Called when an error occurs. #onSucces() wont be called in case of error
         * @param e error
         */
        void onError(Exception e);

        /**
         * Called when everything is uploaded
         */
        void onSuccess();
    }

}
