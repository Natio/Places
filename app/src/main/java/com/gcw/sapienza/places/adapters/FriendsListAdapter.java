package com.gcw.sapienza.places.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by snowblack on 3/2/15.
 */
public class FriendsListAdapter extends ArrayAdapter<String> {

    private static final String TAG = "FriendsListAdapter";

    private View v;

    public FriendsListAdapter(Context context, int resource, List<String> friends) {
        super(context, resource, friends);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        v = convertView;

        if (v == null) {

            v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.friend_layout, parent, false);

        }

        final String friendId = getItem(position);

        final TextView friendNameView = (TextView) v.findViewById(R.id.friend_card_textView_username);
        final ImageView friendImageView = (ImageView) v.findViewById(R.id.friend_card_profile_pic);


        // FIXME Caching temporarily disabled
        // if(!PlacesLoginUtils.getInstance().isUserNameCached(friendId)) {

        ParseQuery<PlacesUser> queryUsers = ParseQuery.getQuery("_User");
        queryUsers.whereEqualTo(PlacesUser.FACEBOOK_ID_KEY, friendId);
        queryUsers.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
        queryUsers.getFirstInBackground(new GetCallback<PlacesUser>() {
            @Override
            public void done(final PlacesUser placesUser, ParseException e) {

                if (e == null) {
                    Log.d(TAG, placesUser.getName());
                    friendNameView.setText(placesUser.getName());
                    PlacesLoginUtils.getInstance().addEntryToUserIdMap(friendId, placesUser.getName());
                    PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(friendId, friendImageView, PlacesLoginUtils.PicSize.LARGE, placesUser.getAccountType());
/*
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "Click");
                            ((MainActivity) getContext()).switchToOtherFrag(ProfileFragment.newInstance(placesUser));
                        }
                    });
                    */
                } else {

                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getContext(), "An error occurred while retrieving friends' data", Toast.LENGTH_SHORT).show();

                }
            }
        });


        return v;
    }
}
