package com.gcw.sapienza.places.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gcw.sapienza.places.BuildConfig;
import com.gcw.sapienza.places.R;

/**
 * Created by Simone on 12/30/2014.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingsFragment";
    private static final int AR_THRESHOLD = 10;
    private int ar_sensor;
    private boolean sensorEnabled;
    private boolean firstClick;

    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings_layout);

        Preference loneWolf_filter = findPreference("meFilter");
        Preference fl_filter = findPreference("flFilter");
        Preference strangers_filter = findPreference("strangersFilter");
        Preference time_filter = findPreference("timeFilter");
        Preference max_fetch = findPreference("maxFetch");
        Preference version_label = findPreference("version");
        Preference notifications_check = findPreference("notificationsCheck");

        version_label.setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ')');
        loneWolf_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        fl_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        strangers_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        time_filter.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        max_fetch.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        max_fetch.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
        notifications_check.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());

        version_label.setOnPreferenceClickListener(this);
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
        if (preference.getKey().equals("version") && sensorEnabled) {
            ar_sensor++;

            if (ar_sensor >= AR_THRESHOLD) {
                // TODO there will be more
                showAR();
                sensorEnabled = false;
            }

            return true;
        }
        return false;
    }

    //Easter egg Simone
    private void showAR() {
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

    private void getRidOfDialogAndShowToast() {
        Toast.makeText(getActivity(), "Simone likes this.", Toast.LENGTH_LONG).show();

        dialog.dismiss();
    }

    class CustomListener implements View.OnClickListener {
        private final AlertDialog dialog;

        public CustomListener(AlertDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            Button b = (Button) v;

            if (b.getText().equals("No")) {
                if (firstClick) {
                    b.setText("Yes");

                    firstClick = false;
                } else getRidOfDialogAndShowToast();
            } else getRidOfDialogAndShowToast();
        }
    }
}
