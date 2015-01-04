package com.gcw.sapienza.places;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by Simone on 12/30/2014.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings_layout);

        Preference loneWolf_filter = (Preference) findPreference("meFilter");
        Preference fl_filter = (Preference) findPreference("flFilter");
        Preference strangers_filter = (Preference) findPreference("strangersFilter");
        Preference time_filter = (Preference) findPreference("timeFilter");
        Preference seek_bar = (Preference) findPreference("seekBar");

        loneWolf_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        fl_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        strangers_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        time_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        seek_bar.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
    }
}
