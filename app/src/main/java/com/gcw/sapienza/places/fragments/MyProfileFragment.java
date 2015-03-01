package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

/**
 * Created by snowblack on 2/26/15.
 */
public class MyProfileFragment extends Fragment {

    private static final String TAG = "MyProfileFragment";

    private ImageView fbPicView;
    private TextView fbNameView;
    private TextView postsView;
    private TextView wowedView;

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
        postsView = (TextView)view.findViewById(R.id.postsView);
        wowedView = (TextView)view.findViewById(R.id.wowedView);


        FacebookUtils.getInstance().getFacebookUsernameFromID(PlacesLoginUtils.getInstance().getCurrentUserId(), new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                fbNameView.setText("User name: " + result);
            }
        });

        FacebookUtils.getInstance().loadProfilePicIntoImageView(PlacesLoginUtils.getInstance().getCurrentUserId(), fbPicView, PlacesLoginUtils.PicSize.LARGE);

        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        query.whereEqualTo("fbId", PlacesLoginUtils.getInstance().getCurrentUserId());
        query.countInBackground(new CountCallback() {
            @Override
            public void done(int i, ParseException e) {
                if(e == null) {
                    postsView.setText("Flags placed: " + i);
                }else{
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        return view;
    }
}
