package com.gcw.sapienza.places;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();

        if(preference.getKey().equals("meFilter") ||
                preference.getKey().equals("flFilter") ||
                preference.getKey().equals("strangersFilter") ||
                preference.getKey().equals("timeFilter"))
        {
            Log.d(TAG, "Called onPreferenceChange for: " + preference.getKey());
            editor.putBoolean(preference.getKey(), (boolean)newValue);
            editor.commit();
        }
        else if(preference.getKey().equals("seekBar"))
        {
            preference.setDefaultValue(newValue);

            int value = (int)newValue + 1;

            Utils.MAP_RADIUS = value / 10f;
            Utils.mainActivity.getMosaicFragment().updateHeaderText();

            showToast("Radius set to " + value * 100 + " meters.");

            Log.d(TAG, "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }
        else if(preference.getKey().equals("maxFetch"))
        {
            preference.setDefaultValue(newValue);

            int value = Utils.stepValues[(int)newValue];

            Utils.MAX_PINS = value;
            Utils.mainActivity.getMosaicFragment().updateHeaderText();

            showToast("Max number of visible flags: " + value + '.');

            Log.d(TAG, "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }

        Location currentLocation = PlacesApplication.getInstance().getLocation();
        PlacesApplication.getInstance().getLocationService().queryParsewithLocation(currentLocation);

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
