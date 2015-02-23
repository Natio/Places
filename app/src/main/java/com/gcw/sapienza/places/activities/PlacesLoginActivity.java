package com.gcw.sapienza.places.activities;

import com.parse.ui.ParseLoginActivity;

/**
 * Created by paolo on 23/02/15.
 */
public class PlacesLoginActivity extends ParseLoginActivity {
    @Override
    public void onBackPressed() {
        //DO NOT call super.onBackPressed
        //to avoid dismissing login
        //view without logging in
    }
}
