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
import com.gcw.sapienza.places.utils.Utils;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

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
    private TextView flagsView;
    private TextView categoryView;
    private TextView wowedView;
    private Button friendsView;

    private int numFlags;

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
        flagsView = (TextView) view.findViewById(R.id.flagsView);
        categoryView = (TextView) view.findViewById(R.id.categoryView);
        wowedView = (TextView) view.findViewById(R.id.wowedView);
        friendsView = (Button) view.findViewById(R.id.friendsView);

        FacebookUtils.getInstance().loadProfilePicIntoImageView(this.fbId, fbPicView, PlacesLoginUtils.PicSize.LARGE);

        String[] categories = getActivity().getResources().getStringArray(R.array.categories);
        for(final String cat: categories){
            ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
            query.whereEqualTo("fbId", this.fbId);
            query.whereEqualTo("category", cat);
            query.countInBackground(new CountCallback() {
                @Override
                public void done(int i, ParseException e) {
                    if(e == null) {
                        CharSequence currText = categoryView.getText();
                        if(currText.equals("")){
                            categoryView.setText(" • " + cat + ": " + i);
                        }else{
                            categoryView.setText(currText + "\n • " + cat + ": " + i);
                        }
                        numFlags += i;
                        flagsView.setText("Flags placed: " + numFlags);
                    } else {
                        Log.e(TAG, e.getMessage());
                        Utils.showToast(getActivity(), "An error occurred while retrieving your Flags data", Toast.LENGTH_SHORT);
                    }
                }
            });
        }

        //User posts retrieval above is more efficient
        @Deprecated
//        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
//        query.whereEqualTo("fbId", this.fbId);
//        query.findInBackground(new FindCallback<Flag>() {
//            @Override
//            public void done(List<Flag> flags, ParseException e) {
//                if (e == null) {
//                    String[] categories = getActivity().getResources().getStringArray(R.array.categories);
//                    HashMap<String, Integer> categoriesMap = new HashMap<>();
//                    for(String cat: categories){
//                        Log.d(TAG, "Array category: " + cat);
//                        categoriesMap.put(cat, 0);
//                    }
//                    for(int i = 0; i < flags.size(); i++){
//                        Flag currFlag = flags.get(i);
//                        Log.d(TAG, "Flag category: " + currFlag.getCategory());
//                        int currentNumFlags = categoriesMap.get(currFlag.getCategory());
//                        categoriesMap.put(currFlag.getCategory(), ++currentNumFlags);
//                    }
//                    String categoriesString = "Flags placed: "+ flags.size() + "\n";
//                    for(String cat: categories){
//                        categoriesString += " • " + cat + ": " + categoriesMap.get(cat) + "\n";
//                    }
//                    categoriesString = categoriesString.trim();
//                    flagsView.setText(categoriesString);
//                } else {
//                    Log.e(TAG, e.getMessage());
//                    Toast.makeText(getActivity(), "There was a problem retrieving your Flags", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });


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
