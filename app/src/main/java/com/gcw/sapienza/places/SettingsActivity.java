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
            int value = (int)newValue + 1;

            Utils.MAP_RADIUS = value / 10f;
            MosaicFragment.updateHeaderText();

            showToast("Radius set to " + value * 100 + " meters.");

            Log.d("Settings Activity", "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }
        else if(preference.getTitle().equals("The more, the better"))
        {
            int value = Utils.stepValues[(int)newValue];

            Utils.MAX_PINS = value;
            MosaicFragment.updateHeaderText();

            showToast("Max pins allowed set to " + value + ".");

            Log.d("Settings Activity", "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }

        PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());

        return true;
    }

    private void showToast(String text) {
        if(radiusToast != null)
            radiusToast.cancel();
        radiusToast = Toast.makeText(getBaseContext(), text,
                Toast.LENGTH_SHORT);
        radiusToast.show();
    }

}
