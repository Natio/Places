package com.gcw.sapienza.places.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helps in the creation of thumbnail images
 * Created by paolo on 18/02/15.
 */
public  class ThumbnailCreator {
    private static final String TAG = "ThumbnailCreator";
    private static final String THUMB_FORMAT_EXTENSION = ".jpg";
    private static final int DEFAULT_WIDTH = 500;
    private static final int DEFAULT_HEIGHT = 500;

    private ThumbnailCreator(){}

    /**
     * Scales an image maintaining aspect ratio
     * @param original original image
     * @param maxWidth maximum width
     * @param maxHeight maximum height
     * @return a Bitmap representing the thumbnail
     */
    public static Bitmap createThumbnailForImage(Bitmap original, int maxWidth, int maxHeight){
        int width = original.getWidth();
        int height = original.getHeight();

        Log.v("Pictures", "Width and height are " + width + "--" + height);

        if (width > height) {
            // landscape
            float ratio = (float) width / maxWidth;
            width = maxWidth;
            height = (int) (height / ratio);
        } else if (height > width) {
            // portrait
            float ratio = (float) height / maxHeight;
            height = maxHeight;
            width = (int) (width / ratio);
        } else {
            // square
            height = maxHeight;
            width = maxWidth;
        }

        Log.v("Pictures", "after scaling Width and height are " + width + "--" + height);

        return Bitmap.createScaledBitmap(original, width, height, true);
    }




    /**
     * Scales an image maintaining aspect ratio
     * @param original original image file
     * @param maxWidth maximum width
     * @param maxHeight maximum height
     * @return  a File representing the thumbnail
     */
    public static File createThumbnailForImageRespectingProportions(File original, int maxWidth, int maxHeight){
        Bitmap src = BitmapFactory.decodeFile(original.getAbsolutePath());
        Bitmap thumbnail = ThumbnailCreator.createThumbnailForImage(src, maxWidth, maxHeight);

        File thumbFile = ThumbnailCreator.generateThumbnailFileForFile(original, false);
        boolean result = writeBitmapToFile(thumbnail, thumbFile);

        return result ? thumbFile : null;
    }


    /**
     * Scales an image maintaining aspect ratio.
     * @param original image
     * @return a File containing the thumbnail
     */
    public static File createThumbnailForImageRespectingProportions(File original){
        return ThumbnailCreator.createThumbnailForImageRespectingProportions(original, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }


    /**
     * Generates a thumbnail from a video
     * @param video file containing the video
     * @return File containing the thumbnail image
     */
    public static File createTumbnailForVideo(File video){
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(video.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
        File thumbFile = ThumbnailCreator.generateThumbnailFileForFile(video, true);
        boolean result = writeBitmapToFile(thumbnail, thumbFile);
        return result ? thumbFile : null;
    }


    /**
     * Creates a thumbnail file starting from an image/video file
     * @param original_file original file path
     * @param video true if the file is a video. false if the file is a picture
     * @return a file
     */
    private static File generateThumbnailFileForFile(File original_file, boolean video){
            // Create an image file name
        try{
            String imageFileName = "thumb" + original_file.getName();
            Log.d(TAG, original_file.getName());
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    video ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES);
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    THUMB_FORMAT_EXTENSION,         /* suffix */
                    storageDir      /* directory */
            );
        }
        catch (IOException e){
            Log.d(TAG, "Error", e);
        }
        return null;

    }


    /**
     * Writes an image to file
     * @param src the image to write
     * @param file the destination file
     * @return true in case of success, false otherwise
     */
    private static boolean writeBitmapToFile(Bitmap src, File file){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            src.compress(Bitmap.CompressFormat.JPEG, 70, out); // bmp is your Bitmap instance
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
