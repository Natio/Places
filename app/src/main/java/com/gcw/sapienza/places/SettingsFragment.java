package com.gcw.sapienza.places;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Simone on 12/30/2014.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingsFragment";

    private int easterCount;
    private boolean eggEnabled;
    private final int EASTER_THRESHOLD = 10;
    private boolean firstClick;

    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firstClick = true;

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings_layout);

        Preference loneWolf_filter =  findPreference("meFilter");
        Preference fl_filter = findPreference("flFilter");
        Preference strangers_filter = findPreference("strangersFilter");
        Preference time_filter = findPreference("timeFilter");
        Preference seek_bar = findPreference("seekBar");
        Preference max_fetch = findPreference("maxFetch");
        Preference version_label = findPreference("version");

        Preference thoughts_check =  findPreference("thoughtsCheck");
        Preference fun_check =  findPreference("funCheck");
        Preference landscape_check =  findPreference("landscapeCheck");
        Preference food_check =  findPreference("foodCheck");
        Preference none_check =  findPreference("noneCheck");

        String version_name = BuildConfig.VERSION_NAME;
        int version_code = BuildConfig.VERSION_CODE;

        version_label.setSummary(version_name + " ("+version_code+")");
        loneWolf_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        fl_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        strangers_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        time_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        seek_bar.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        max_fetch.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());

        thoughts_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        fun_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        landscape_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        food_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());
        none_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)getActivity());

        version_label.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.easterCount = 0;
        this.eggEnabled = true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        if(preference.getKey().equals("version") && eggEnabled)
        {
            easterCount++;

            if(easterCount >= EASTER_THRESHOLD)
            {
                showEasterEgg();
                eggEnabled = false;
            }

            return true;
        }

        return false;
    }

    private void showEasterEgg()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.custom_dialog_layout, null))
                .setTitle("Is this the man of the year?")
                .setPositiveButton("Yes", null)
                .setNegativeButton("No", null)
                .setCancelable(false);

        dialog = builder.create();
            dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new CustomListener(dialog));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new CustomListener(dialog));
    }

    class CustomListener implements View.OnClickListener
    {
        private final AlertDialog dialog;

        public CustomListener(AlertDialog dialog)
        {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;

            if(b.getText().equals("No"))
            {
                if (firstClick)
                {
                    b.setText("Yes");

                    firstClick = false;
                }
                else getRidOfDialogAndShowToast();
            }
            else getRidOfDialogAndShowToast();
        }
    }

    private void getRidOfDialogAndShowToast()
    {
        Toast.makeText(getActivity(), "Simone likes this.", Toast.LENGTH_LONG).show();

        dialog.dismiss();
    }
}
