package com.gcw.sapienza.places;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by Simone on 12/30/2014.
 */
public class SettingsFragment extends PreferenceFragment {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings_layout);

        Preference loneWolf_filter =  findPreference("meFilter");
        Preference fl_filter = findPreference("flFilter");
        Preference strangers_filter = findPreference("strangersFilter");
        Preference time_filter = findPreference("timeFilter");
        Preference seek_bar = findPreference("seekBar");
        Preference max_fetch = findPreference("maxFetch");
        Preference version_label = findPreference("version");

        String version_name = BuildConfig.VERSION_NAME;
        int version_code = BuildConfig.VERSION_CODE;

        version_label.setSummary(version_name + " ("+version_code+")");
        loneWolf_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        fl_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        strangers_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        time_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        seek_bar.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        max_fetch.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());

    }
}
