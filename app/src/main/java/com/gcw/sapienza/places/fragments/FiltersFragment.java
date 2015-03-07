package com.gcw.sapienza.places.fragments;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.gcw.sapienza.places.R;

/**
 * Created by Simone on 12/30/2014.
 */
public class FiltersFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingsFragment";

    private int ar_sensor;
    private boolean sensorEnabled;
    private boolean firstClick;

    //private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.categories_filter_list);

        Preference thoughts_check = findPreference("thoughtsCheck");
        Preference fun_check = findPreference("funCheck");
        Preference music_check = findPreference("musicCheck");
        Preference landscape_check = findPreference("landscapeCheck");
        Preference food_check = findPreference("foodCheck");
        Preference none_check = findPreference("noneCheck");

        thoughts_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        fun_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        music_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        landscape_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        food_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        none_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());

    }

    @Override
    public void onResume() {
        super.onResume();

        this.firstClick = true;
        this.ar_sensor = 0;
        this.sensorEnabled = true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("noneCheck") && sensorEnabled) {
            return true;
        }
        return false;
    }


}
