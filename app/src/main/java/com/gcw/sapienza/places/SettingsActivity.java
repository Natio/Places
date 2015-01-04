package com.gcw.sapienza.places;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gcw.sapienza.places.utils.Utils;

/**
 * Created by Simone on 12/19/2014.
 */
public class SettingsActivity extends Activity
        implements Preference.OnPreferenceChangeListener{

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        preference.setDefaultValue(newValue);

        if(preference.getTitle().equals("Lone Wolf"))
        {
            Utils.LONE_WOLF_ENABLED = (boolean)newValue;
        }
        else if(preference.getTitle().equals("With Friends Surrounded"))
        {
            Utils.WITH_FRIENDS_SURROUNDED_ENABLED = (boolean)newValue;
        }
        else if(preference.getTitle().equals("Storytellers In The Dark"))
        {
            Utils.STORYTELLERS_IN_THE_DARK_ENABLED = (boolean)newValue;
        }
        else if(preference.getTitle().equals("Archaeologist"))
        {
            Utils.ARCHAEOLOGIST_ENABLED = (boolean)newValue;
        }
        else if(preference.getTitle().equals("How far will you see?"))
        {
            //TODO make these settings work...visually it seems more broken than it actually is:
            //TODO moving the bar actually updates the app radius
            Log.d(TAG, "SeekBar stored value: " + String.valueOf(preferences.getInt("seekBar", 3)));
            int value = (int)newValue;
            Utils.MAP_RADIUS = value / 10f;
            MosaicFragment.updateHeaderText();
            Log.d("Settings Activity", "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }

        PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());

        return true;
    }

}
