package com.gcw.sapienza.places;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.utils.Utils;

/**
 * Created by Simone on 12/19/2014.
 */
public class SettingsActivity extends Activity
        implements Preference.OnPreferenceChangeListener{

    private static final String TAG = "SettingsActivity";

    Toast radiusToast;

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
        else if(preference.getTitle().equals("How far will you see?"))
        {
            int value = (int)newValue + 1;

            Utils.MAP_RADIUS = value / 10f;
            MosaicFragment.updateHeaderText();

            showToast(value);

            Log.d("Settings Activity", "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);

        }
        return true;
    }

    private void showToast(int value) {
        if(radiusToast != null)
            radiusToast.cancel();
        radiusToast = Toast.makeText(getBaseContext(), "Radius set to " + value * 100 + " meters.",
                Toast.LENGTH_SHORT);
        radiusToast.show();
    }

}
