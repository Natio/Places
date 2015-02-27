package com.gcw.sapienza.places.utils;

import android.content.Context;

/**
 * Created by mic_head on 27/02/15.
 */
public class PlacesLoginUtils {

    public enum LoginType{ FACEBOOK, GPLUS }

    public static LoginType loginType;

    public static void downloadUserInfo(Context context)
    {
        if(loginType == LoginType.FACEBOOK) FacebookUtils.downloadFacebookInfo(context);
    }

}
