package com.gcw.sapienza.places.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;

/**
 * Created by snowblack on 3/8/15.
 */
public class CategoriesFragment extends Fragment {

    public static final String TAG = CategoriesFragment.class.getName();

    private static final String ENABLE_ALL_STRING = "ENABLE ALL";
    private static final String DISABLE_ALL_STRING = "DISABLE ALL";

    public static final String ENABLE_ALL_CLICKED = "Enable All Clicked";

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.categories_screen, container, false);

        RelativeLayout categoriesLayout = (RelativeLayout) view.findViewById(R.id.categoriesLayout);
        RelativeLayout categoriesSettings = (RelativeLayout) view.findViewById(R.id.categoriesSettings);

        Button enableAllButton = (Button)view.findViewById(R.id.enableAllButton);
        enableAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Clicked!");

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = preferences.edit();

                Button btn = (Button)v;
                CharSequence btnText = btn.getText();

                if(btnText.equals(ENABLE_ALL_STRING)){
                    editor.putBoolean("noneCheck", true);
                    editor.putBoolean("thoughtsCheck", true);
                    editor.putBoolean("funCheck", true);
                    editor.putBoolean("musicCheck", true);
                    editor.putBoolean("foodCheck", true);
                    editor.putBoolean("landscapeCheck", true);
                    editor.commit();
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(CategoriesFragment.ENABLE_ALL_CLICKED));
                    btn.setText(DISABLE_ALL_STRING);
                }else{
                    editor.putBoolean("noneCheck", false);
                    editor.putBoolean("thoughtsCheck", false);
                    editor.putBoolean("funCheck", false);
                    editor.putBoolean("musicCheck", false);
                    editor.putBoolean("foodCheck", false);
                    editor.putBoolean("landscapeCheck", false);
                    editor.commit();
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(CategoriesFragment.ENABLE_ALL_CLICKED));
                    btn.setText(ENABLE_ALL_STRING);
                }
            }
        });

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(MainActivity.PREFERENCES_CHANGED_NOTIFICATION));

        updateEnableAllButton();

        getActivity().getFragmentManager().beginTransaction().replace(R.id.categoriesLayout, new FiltersFragment()).commit();

        return view;
    }

    private void updateEnableAllButton() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Button enableAllButton = (Button)view.findViewById(R.id.enableAllButton);

        if(preferences.getBoolean("noneCheck", true) && preferences.getBoolean("thoughtsCheck", true) &&
                preferences.getBoolean("funCheck", true) && preferences.getBoolean("musicCheck", true) &&
                preferences.getBoolean("foodCheck", true) && preferences.getBoolean("landscapeCheck", true)){
            enableAllButton.setText(DISABLE_ALL_STRING);
        }else{
            enableAllButton.setText(ENABLE_ALL_STRING);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case MainActivity.PREFERENCES_CHANGED_NOTIFICATION:
                    updateEnableAllButton();
                    break;

                default:
                    Log.w(CategoriesFragment.class.getName(), intent.getAction() + ": cannot identify the received notification");
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }
}
