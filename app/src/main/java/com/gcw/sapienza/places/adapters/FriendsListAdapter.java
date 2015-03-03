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
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.fragments.ProfileFragment;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by snowblack on 3/2/15.
 */
public class FriendsListAdapter extends ArrayAdapter<String> {

    private static final String TAG = "FriendsListAdapter";

    public FriendsListAdapter(Context context, int resource, List<String> friends) {
        super(context, resource, friends);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {

            v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.friend_layout, parent, false);

        }

        final String friendId = getItem(position);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getContext()).switchToOtherFrag(new ProfileFragment().newInstance(friendId));
            }
        });

        final TextView friendNameView = (TextView) v.findViewById(R.id.friend_card_textView_username);
        final ImageView friendImageView = (ImageView) v.findViewById(R.id.friend_card_profile_pic);



        if(!PlacesLoginUtils.getInstance().isUserNameCached(friendId)) {

            ParseQuery<PlacesUser> queryUsers = ParseQuery.getQuery("_User");
            queryUsers.whereEqualTo(PlacesUser.FACEBOOK_ID_KEY, friendId);
            queryUsers.getFirstInBackground(new GetCallback<PlacesUser>() {
                @Override
                public void done(PlacesUser placesUser, ParseException e) {

                    if (e == null) {

                        friendNameView.setText(placesUser.getName());
                        PlacesLoginUtils.getInstance().addEntryToUserIdMap(friendId, placesUser.getName());

                    } else {

                        Log.e(TAG, e.getMessage());
                        Toast.makeText(getContext(), "An error occurred while retrieving friends' data", Toast.LENGTH_SHORT).show();

                    }
                }
            });

        }else{
            String friendName = PlacesLoginUtils.getInstance().getUserNameFromId(friendId);
            friendNameView.setText(friendName);
        }

        PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(friendId, friendImageView, PlacesLoginUtils.PicSize.LARGE);

        return v;
    }

}
