package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by snowblack on 2/26/15.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private static final String FBID = "FacebookID";

    private String fbId;

    private ImageView fbPicView;
    private TextView fbNameView;
    private TextView memberSinceView;
    private TextView postsView;
    private TextView wowedView;
    private Button friendsView;

    public static final ProfileFragment newInstance(String fbId) {

        ProfileFragment fragment = new ProfileFragment();

        Bundle bundle = new Bundle();
        bundle.putString(FBID, fbId);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.fbId = getArguments().getString(FBID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.profile_layout, container, false);

        fbPicView = (ImageView) view.findViewById(R.id.fbPicView);
        fbNameView = (TextView) view.findViewById(R.id.fbNameView);
        memberSinceView = (TextView) view.findViewById(R.id.memberSinceView);
        postsView = (TextView) view.findViewById(R.id.postsView);
        wowedView = (TextView) view.findViewById(R.id.wowedView);
        friendsView = (Button) view.findViewById(R.id.friendsView);

        FacebookUtils.getInstance().loadProfilePicIntoImageView(this.fbId, fbPicView, PlacesLoginUtils.PicSize.LARGE);

        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        query.whereEqualTo("fbId", this.fbId);
        query.countInBackground(new CountCallback() {
            @Override
            public void done(int i, ParseException e) {
                if (e == null) {
                    postsView.setText("Flags placed: " + i);
                } else {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getActivity(), "There was a problem retrieving your Flags", Toast.LENGTH_SHORT).show();
                }
            }
        });


        ParseQuery<PlacesUser> queryUsers = ParseQuery.getQuery("_User");
        queryUsers.whereEqualTo(PlacesUser.FACEBOOK_ID_KEY, this.fbId);
        PlacesUser user = null;
        try {
            user = queryUsers.getFirst();
            fbNameView.setText("User name: " + user.getName());

            DateFormat dateFormatter = new SimpleDateFormat("EEE, d MMMM yyyy");
            String formattedDate = dateFormatter.format(user.getCreatedAt().getTime());
            memberSinceView.setText("Placer since: " + formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while retrieving user data", Toast.LENGTH_SHORT).show();
        }

        ParseQuery wows = ParseQuery.getQuery("Wow_Lol_Boo");
        wows.whereEqualTo("user", user);
        wows.countInBackground(new CountCallback() {
            @Override
            public void done(int i, ParseException e) {
                if (e == null) {
                    wowedView.setText("Flags wowed: " + i);
                } else {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getActivity(), "An error occurred while retrieving social data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (!PlacesLoginUtils.getInstance().getCurrentUserId().equals(this.fbId)) {
            friendsView.setVisibility(View.INVISIBLE);
        }

        friendsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).switchToOtherFrag(new MyFriendsFragment());
            }
        });

        return view;
    }
}