package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcw.sapienza.places.R;

/**
 * Created by snowblack on 2/16/15.
 */
public class NoFlagsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.no_flags_layout, container, false);

        return view;
    }
}
