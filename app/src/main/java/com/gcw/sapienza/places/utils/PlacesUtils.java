/*
 * Copyright 2015-present Places®.
 */
package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.gcw.sapienza.places.R;

import java.io.File;
import java.io.IOException;

/**
 * PlacesUtils is a list of functions that are used in the Places platform. The aim of PlacesUtils
 * is to modularize the software.
 */
public class PlacesUtils {
    public static final int UPDATE_DELAY = 200;
    public static final int ANIMATION_DURATION = 300;
    public static final int MAP_BOUNDS = 70;
    public static final float ZOOM_LVL = 16f;
    public static final int VID_SHOOT_REQUEST_CODE = 90;
    public static final int PIC_CAPTURE_REQUEST_CODE = 91;
    public static final int RECORD_AUDIO_REQUEST_CODE = 92;
    public static final int PHONE_AUDIO_REQUEST_CODE = 93;
    public static final int PHONE_IMAGE_REQUEST_CODE = 94;
    public static final int PHONE_VIDEO_REQUEST_CODE = 95;
    public static final int SHARE_SOCIAL_REQUEST_CODE = 1001;
    public static final int PHONE_AUDIO = 0;
    public static final int PHONE_IMAGE = 1;
    public static final int PHONE_VIDEO = 2;
    @SuppressWarnings("unused")
    public static final int SETTINGS_REQUEST_CODE = 70;
    public static final int GPS_ENABLE_REQUEST_CODE = 71;
    public static final int LOGIN_REQUEST_CODE = 72;
    public static final int VIBRATION_DURATION = 0; // I didn't like it that much
    public static final int FLAG_LIST_GROUP = 0;
    public static final int PHONE_MEDIA_GROUP = 1;
    protected static final int CHUNK_SIZE = 4096;

    public static final String NO_VALID_FLAG_SELECTED = "No valid flag selected";
    public static final String FLAG_DELETED = "Flag deleted";
    public static final String FLAG_REPORTED = "Flag reported";
    public static final String FLAG_REPORT_REVOKED = "Flag report revoked";
    public static final int DELETE_FLAG = 0;
    public static final int REPORT_FLAG = 1;
    public static final int DELETE_REPORT_FLAG = 2;

    public static final String NO_VALID_COMMENT_SELECTED = "No valid comment selected";
    public static final String COMMENT_DELETED = "Comment deleted";
    public static final String COMMENT_REPORTED = "Comment reported";
    public static final String COMMENT_REPORT_REVOKED = "Comment report revoked";
    public static final String RECEIVED_NOTIF_COMMENT_TYPE = "comment_notification";
    public static final String RECEIVED_MULTI_NOTIF_COMMENT_TYPE = "multi_comment_notification";
    public static final String FLAG_ID = "flagId";
    public static final int DELETE_COMMENT = 3;
    public static final int REPORT_COMMENT = 4;
    public static final int DELETE_REPORT_COMMENT = 5;
    public static final int COMMENT_LIST_GROUP = 6;

    @SuppressWarnings("unused")
    private static final String TAG = "Utils";
    /**
     * as the radius settings have been deleted,
     * the static map radius is now set to 150 meters
     */
    public static final float MAP_RADIUS = 0.15f;
    /**
     * the discover mode radius is double as the map radius
     */
    public static final float DISCOVER_MODE_RADIUS = MAP_RADIUS * 2;

    public static final int MAX_FLAGS_DEFAULT_INDEX = 1;
    public static final int[] STEP_VALUES = {10, 20, 30, 40, 50};

    public static final float FLAG_ALPHA_NORMAL = 0.75f;
    public static final float FLAG_ALPHA_UNSELECTED = 0.4f;
    public static final float FLAG_ALPHA_SELECTED = 1f;
    public static final float FLAG_APLHA_HIDDEN = 0.25f;

    public static final float FLAG_SCALE_NORMAL = 0.25f;

    public static final int NEARBY_FLAGS_CODE = 51;
    public static final int MY_FLAGS_CODE = 52;
    public static final int BAG_FLAGS_CODE = 53;
    public static final int DEFAULT_FLAGS_CODE = 54;

    /**
     * Returns a string containing the name of the file without the extension
     *
     * @param f file
     * @return the name W/o extension of the file
     */
    @SuppressWarnings("UnusedDeclaration")
    protected static String getNameFromFile(File f) {
        String filenameArray[] = f.getName().split("\\.");
        if (filenameArray.length == 1) return "";
        return filenameArray[filenameArray.length - 2];

    }

    /**
     * Returns a string containing the extension of the file
     *
     * @param f file
     * @return the extension of the file
     */
    protected static String getExtensionFromFile(File f) {
        String filenameArray[] = f.getName().split("\\.");
        if (filenameArray.length == 0) return "";
        return filenameArray[filenameArray.length - 1];
    }

    public static String generateRandomName() {
        return "_" + System.currentTimeMillis();
    }

    public static File createAudioFile(String extension, Context ctx) throws IOException {

        String imageFileName = 'a' + PlacesUtils.generateRandomName();

        File cache_dir = ctx.getExternalCacheDir();
        return File.createTempFile(
                imageFileName,
                extension,
                cache_dir
        );
    }

    public static File createImageFile(String image_extension) throws IOException {
        // Create an image file name
        String imageFileName = "img" + PlacesUtils.generateRandomName();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                image_extension,         /* suffix */
                storageDir      /* directory */
        );
    }

    public static File createRecordingVideoFile(String extension) throws IOException {
        String imageFileName = "vid" + PlacesUtils.generateRandomName();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                extension,         /* suffix */
                storageDir      /* directory */
        );

    }

    public static String getVideoRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Video.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public static int getIconForCategory(String category, Context context) {
        String[] category_array = context.getResources().getStringArray(R.array.categories);

        if (category == null || category.equals(category_array[0]))
            return R.drawable.flag_red; //None
        else if (category.equals(category_array[1])) return R.drawable.flag_green; //Thoughts
        else if (category.equals(category_array[2])) return R.drawable.flag_yellow; //Fun
        else if (category.equals(category_array[3])) return R.drawable.flag_blue; //Music
        else if (category.equals(category_array[4])) return R.drawable.flag_grey; //Landscape
        else return R.drawable.flag_purple; //Food
    }

    public static String getImageRealPathFromURI(Context context, Uri uri) {

        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void showToast(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

}
