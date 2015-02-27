package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;

/**
 * Created by snowblack on 2/26/15.
 */
public class MyProfleFragment extends Fragment {

    private ImageView fbPicView;
    private TextView fbNameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_profile_layout, container, false);

        fbPicView = (ImageView)view.findViewById(R.id.fbPicView);
        fbNameView = (TextView)view.findViewById(R.id.fbNameView);

        FacebookUtils.getInstance().getFacebookUsernameFromID(PlacesLoginUtils.getInstance().getCurrentUserId(), new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                fbNameView.setText("User name: " + result);
            }
        });

        FacebookUtils.getInstance().loadProfilePicIntoImageView(PlacesLoginUtils.getInstance().getCurrentUserId(), fbPicView, PlacesLoginUtils.PicSize.LARGE);

        return view;
    }
}
