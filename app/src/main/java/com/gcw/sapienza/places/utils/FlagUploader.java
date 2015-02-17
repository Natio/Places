package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.gcw.sapienza.places.BuildConfig;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;


/**
 * This class allows to asynchronously upload a Flag and to have completion callbacks
 *
 *
 *  Usage:
 *
 *      File video;
 *      File audio;
 *      File picture;
 *
 *      FlagUploader up = new FlagUploader();
 *      up.setVideoFile(video);
 *      up.setAudioFile(audio);
 *      up.setPictureFile(picture);
 *
 *      uploader.upload(new FlagUploaderCallbacks{...});
 *
 *
 *  How It Works
 *
 *  When upload() is called FlagUploader.loadFileLoop() is automatically invoked.
 *  FlagUploader.loadFileLoop() is the responsible of ParseFile creation and loading.
 *
 *  1) loadFileLoop() checks that this.files is not empty
 *  2) loadFileLoop() removes a File from this.files (that is the Files to upload storage)and schedules a FileLoaderTask that loads the file
 *  in memory and creates a ParseFile.
 *  3) When the ParseFile is configured FileLoaderTask calls FlagUploader onParseFileInMemoryLoadDone(ParseFile parse_file)
 *  4) onParseFileInMemoryLoadDone starts the ParseFile upload; when the upload is finished control returns to 1.
 *
 *  5) if this.files is empty loadFlag() will be called and will upload the flag to Parse.com
 *  6) in case of success onFinish() will be called otherwise onError()
 *
 *
 * Created by paolo on 12/01/15.
 */
public class FlagUploader {
    private static final String TAG = "FlagUploader";

    private static final String AUDIO_KEY = Flag.AUDIO_KEY;
    private static final String VIDEO_KEY = Flag.VIDEO_KEY;
    private static final String PICTURE_KEY = Flag.PICTURE_KEY;
    private static final String PHONE_MEDIA_KEY = Flag.PHONE_MEDIA_KEY;

    private final Flag flag;
    private HashMap<String, File> files;
    private HashMap<String, File> usedFiled;
    private boolean isUploading;
    private FlagUploaderCallbacks callbacks;
  //  private boolean deleteFilesOnFinish = false;

    private String currentFileKey = null;


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
        this.files = new HashMap<>(3);
        this.usedFiled = new HashMap<>(3);
        this.context = ctx;
    }

    /**
     *
     * @return true if method upload has been already called, false otherwise
     */
    @SuppressWarnings("UnusedDeclaration")
    public boolean isUploading(){
        return this.isUploading;
    }

    /**
     * Sets the ParseFile representing a video
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param video file to upload as a video
     */
    public void setVideoFile(File video){
        if(this.isUploading){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(VIDEO_KEY, video);
    }

    /**
     * Sets the ParseFile representing a audio rec
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param audio file to upload as a audio rec
     */
    public void setAudioFile(File audio){
        if(this.isUploading){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(AUDIO_KEY, audio);
    }

    /**
     * Sets the ParseFile representing a picture
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param picture file to upload as a picture
     */
    public void setPictureFile(File picture){
        if(this.isUploading){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(PICTURE_KEY, picture);
    }

    /**
     * Sets the ParseFile representing a picture
     * @throws java.lang.IllegalStateException if you call this method after having started uploading
     * @param media file to upload as a phone media
     */
    public void setPhoneMediaFile(File media){
        if(this.isUploading){
            throw new IllegalStateException("Cannot set a file while uploading");
        }
        this.files.put(PHONE_MEDIA_KEY, media);
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

        FacebookUtils.getInstance().getFacebookUsernameFromID(FacebookUtils.getInstance().getCurrentUserId(), new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                FlagUploader.this.flag.setFbName(result);
                FlagUploader.this.loadFileLoop();
            }
        });



    }


    /**
     * When this method is called it starts to upload a file asynchronously.
     * When a file is uploaded this method will be called again and a new upload will start until
     *  there will be nothing to upload. If there is nothing to upload the Flag will be saved
     */
    private void loadFileLoop(){
        if(BuildConfig.DEBUG && Looper.getMainLooper().getThread() != Thread.currentThread()){
            throw new RuntimeException("Something went wrong with threads");
        }

        if(this.files.isEmpty()){
            this.loadFlag();
            return;
        }

        String key = this.getNextKeyFromFilesMap();
        this.currentFileKey = key;
        File current_file = this.files.remove(key);
        this.usedFiled.put(key,  current_file);

        new FileLoaderTask().execute(current_file);


    }

    /**
     * Called when an error occurs. If this method is called the upload is stopped and no data will be created on parse.com
     * @param e error description
     */

    private void onError(Exception e){
        this.callbacks.onError(e);
    }


    /**
     * Called when a ParseFile is successfully loaded in memory
     * @param parse_file parse file to upload
     */
    private void onParseFileInMemoryLoadDone(ParseFile parse_file){
        if(parse_file == null){
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
                if(e != null){
                    FlagUploader.this.onError(e);
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
        // FlagUploader.this.callbacks.onPercentage(100, "Loading last bits :)");
        System.gc();
        this.flag.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null){
                    FlagUploader.this.onError(e);
                }
                else{
                    FlagUploader.this.onFinish();
                }
            }
        });

    }

    /**
     * Called when everything is uploaded. If #deleteOnFinish is true all files will be deleted
     */
    private void onFinish(){
        FlagUploader.this.callbacks.onSuccess();
        /*if(this.deleteFilesOnFinish){
            for(String key : this.usedFiled.keySet()){
                File f = this.usedFiled.get(key);
                Log.d(TAG, "Deleted file: "+f.getName()+ " ? "+ f.delete());
            }
        }*/
        File f = this.usedFiled.get(AUDIO_KEY);
        if(f != null){
            Log.d(TAG, "Deleted file: "+f.getName()+ " ? " + f.delete());
        }
        this.usedFiled.clear();
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
            case PHONE_MEDIA_KEY:
                return this.context.getString(R.string.upload_phone_media_progress);
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
        else if(this.files.containsKey(PHONE_MEDIA_KEY)){
            return PHONE_MEDIA_KEY;
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



    private class FileLoaderTask extends AsyncTask<File, Integer, ParseFile>{
        @Override
        protected ParseFile doInBackground(File... params) {
            if(params.length == 0){
                return null;
            }

            File file = params[0];

            try{
                return new ParseFile(file.getName(), this.loadFileInMemory(file));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }


        private byte[] loadFileInMemory(File f) throws IOException{
            FileInputStream is = new FileInputStream(f);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            byte[] buff = new byte[Utils.CHUNK_SIZE];
            int i;
            while ((i = is.read(buff, 0, buff.length)) > 0){
                outStream.write(buff, 0, i);
            }
            return outStream.toByteArray();
        }

        @Override
        protected void onPostExecute(ParseFile file) {
            FlagUploader.this.onParseFileInMemoryLoadDone(file);
        }
    }

}
