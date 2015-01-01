package com.gcw.sapienza.places;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;

/**
 * Created by Simone on 12/19/2014.
 */
public class SettingsActivity extends Activity implements Preference.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        if(preference.getTitle().equals("Lone Wolf"))
        {

        }
        else if(preference.getTitle().equals("With Friends Surrounded"))
        {

        }
        else if(preference.getTitle().equals("Storytellers In The Dark"))
        {

        }
        else if(preference.getTitle().equals("Archaeologist"))
        {

        }

        return false;
    }
}
