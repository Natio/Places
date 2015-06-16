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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.Category;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.parse.CountCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by snowblack on 2/26/15.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView flagsView;
    private TextView wowedView;


    private TextView nonCat;
    private TextView thoughtsCat;
    private TextView funCat;
    private TextView landscapeCat;
    private TextView foodCat;
    private TextView musicCat;
    private int numFlags;
    private PlacesUser user;



    public static ProfileFragment newInstance(PlacesUser user) {

        ProfileFragment fragment = new ProfileFragment();
        fragment.user = user;

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);


        View view = inflater.inflate(R.layout.profile_layout, container, false);

        final ImageView fbPicView = (ImageView) view.findViewById(R.id.fbPicView);
        final TextView fbNameView = (TextView) view.findViewById(R.id.fbNameView);
        final TextView memberSinceView = (TextView) view.findViewById(R.id.memberSinceView);
        flagsView = (TextView) view.findViewById(R.id.flagsView);
        //categoryView = (TextView) view.findViewById(R.id.categoryView);
        wowedView = (TextView) view.findViewById(R.id.wowedView);
        final TextView numFollowersView = (TextView) view.findViewById(R.id.numFollowersView);
        final Button friendsView = (Button) view.findViewById(R.id.friendsView);

        final LinearLayout followersLayout = (LinearLayout)view.findViewById(R.id.followers_layout);

        //new split views for categories
        nonCat = (TextView) view.findViewById(R.id.cntNone);
        thoughtsCat = (TextView) view.findViewById(R.id.cntThoughts);
        funCat = (TextView) view.findViewById(R.id.cntFun);
        landscapeCat = (TextView) view.findViewById(R.id.cntLandscape);
        foodCat = (TextView) view.findViewById(R.id.cntFood);
        musicCat = (TextView) view.findViewById(R.id.cntMusic);
        //icons in image views
        ImageView nonCatIco = (ImageView) view.findViewById(R.id.icoNone);
        ImageView thoughtsCatIco = (ImageView) view.findViewById(R.id.icoThoughts);
        ImageView funCatIco = (ImageView) view.findViewById(R.id.icoFun);
        ImageView landscapeCatIco = (ImageView) view.findViewById(R.id.icoLandscape);
        ImageView foodCatIco = (ImageView) view.findViewById(R.id.icoFood);
        ImageView musicCatIco = (ImageView) view.findViewById(R.id.icoMusic);

        nonCatIco.setImageResource(R.drawable.uncategorized);
        thoughtsCatIco.setImageResource(R.drawable.thoughts);
        funCatIco.setImageResource(R.drawable.smile);
        landscapeCatIco.setImageResource(R.drawable.eyes);
        foodCatIco.setImageResource(R.drawable.food);
        musicCatIco.setImageResource(R.drawable.music);


        class UserFetchedHandler{
            public void updateUI(){
                PlacesUser user = ProfileFragment.this.user;
                PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(user.getFbId(), fbPicView, PlacesLoginUtils.PicSize.LARGE, user.getAccountType());

                String[] categories = getActivity().getResources().getStringArray(R.array.categories);


                for (final String cat : categories) {

                    numFlags = 0;

                    ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
                    query.whereEqualTo("fbId", user.getFbId());
                    query.whereEqualTo("category", cat);
                    query.countInBackground(new CountCallback() {
                        @Override
                        public void done(int i, ParseException e) {
                            if (e == null) {

                                if (cat.equals(Category.NONE.toString()))
                                    nonCat.setText(String.valueOf(i));
                                else if (cat.equals(Category.THOUGHTS.toString()))
                                    thoughtsCat.setText(String.valueOf(i));
                                else if (cat.equals(Category.FUN.toString()))
                                    funCat.setText(String.valueOf(i));
                                else if (cat.equals(Category.LANDSCAPE.toString()))
                                    landscapeCat.setText(String.valueOf(i));
                                else if (cat.equals(Category.FOOD.toString()))
                                    foodCat.setText(String.valueOf(i));
                                else
                                    musicCat.setText(String.valueOf(i));

                                numFlags += i;

                                flagsView.setText(numFlags + (numFlags != 1 ? " Flags placed" : " Flag placed"));

                            } else {
                                Log.e(TAG, e.getMessage());
                                PlacesUtils.showToast(getActivity(), "An error occurred while retrieving your Flags data", Toast.LENGTH_SHORT);
                            }
                        }
                    });


                    fbNameView.setText(user.getName());
                    DateFormat dateFormatter = new SimpleDateFormat("EEE, d MMMM yyyy");
                    String formattedDate = dateFormatter.format(user.getCreatedAt().getTime());
                    memberSinceView.setText("Placer since: " + formattedDate);


                }


                ParseQuery wows = ParseQuery.getQuery("Wow_Lol_Boo");
                wows.whereEqualTo("user", user);
                wows.countInBackground(new CountCallback() {
                    @Override
                    public void done(int i, ParseException e) {
                        if (e == null) {
                            wowedView.setText(i + (i != 1 ? " Flags WoWed" : " Flag WoWed"));
                        } else {
                            Log.e(TAG, e.getMessage());
                            Toast.makeText(getActivity(), "An error occurred while retrieving social data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


//                if (PlacesLoginUtils.getInstance().getCurrentUserId().equals(user.getFbId())) {
//                    int numFollowers = PlacesLoginUtils.getInstance().getFriends().size();
//                    numFollowersView.setText(numFollowers + (numFollowers != 1 ? " Placers followed" : " Placer followed"));
//                } else {
//                    numFollowersView.setHeight(0);
//                    numFollowersView.setVisibility(View.INVISIBLE);
//                }

                if (!ParseUser.getCurrentUser().getObjectId().equals(user.getObjectId())) {
                    numFollowersView.setVisibility(View.GONE);
                    followersLayout.setVisibility(View.GONE);
                }else{
                    int numFollowers = PlacesLoginUtils.getInstance().getFriends().size();
                    numFollowersView.setText(numFollowers + (numFollowers != 1 ? " Placers followed" : " Placer followed"));
                    followersLayout.setVisibility(View.VISIBLE);
                }



            }
        }


        final UserFetchedHandler handler = new UserFetchedHandler();

        if(this.user.isDataAvailable()){
            handler.updateUI();
        }
        else{
            this.user.fetchInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {

                    handler.updateUI();
                }
            });
        }

        String currUserId = PlacesLoginUtils.getInstance().getCurrentUserId();
        if(currUserId == null){
            PlacesUtils.showToast(getActivity(), "User data not ready", Toast.LENGTH_SHORT);
            return view;
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
