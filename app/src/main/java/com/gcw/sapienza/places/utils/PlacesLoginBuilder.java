package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.content.Intent;

import com.parse.ui.ParseLoginActivity;
import com.parse.ui.ParseLoginBuilder;
import com.parse.ui.ParseLoginConfig;

import java.util.Arrays;

/**
 * Created by mic_head on 27/02/15.
 */
public class PlacesLoginBuilder extends ParseLoginBuilder {

    private Context context;
    private ParseLoginConfig config = new ParseLoginConfig();

    public PlacesLoginBuilder(Context context)
    {
        super(context);

        this.context = context;

        config.setParseLoginEnabled(false);
        config.setFacebookLoginEnabled(true);
        config.setFacebookLoginPermissions(Arrays.asList("public_profile", "user_friends"/*, "user_relationships", "user_birthday", "user_location"*/));
    }

    public Intent build()
    {
        Intent intent = new Intent(context, ParseLoginActivity.class);
        intent.putExtras(config.toBundle());
        return intent;
    }
}
