package com.gcw.sapienza.places.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gcw.sapienza.places.R;

/**
 * Created by Simone on 12/30/2014.
 */
public class FiltersFragment extends PreferenceFragment {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingsFragment";
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case CategoriesFragment.ENABLE_ALL_CLICKED:
                    setPreferenceScreen(null);
                    loadPreferencesAndSetListeners();
//                    Preference thoughts_check = findPreference("thoughtsCheck");
//                    ((CheckBoxPreference)thoughts_check).setChecked(true);
                    break;

                default:
                    Log.w(FiltersFragment.class.getName(), intent.getAction() + ": cannot identify the received notification");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadPreferencesAndSetListeners();

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(CategoriesFragment.ENABLE_ALL_CLICKED));


    }

    private void loadPreferencesAndSetListeners() {

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
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }


}
