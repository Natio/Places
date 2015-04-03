package com.gcw.sapienza.places.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.fragments.InboxFragment;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by snowblack on 4/2/15.
 */
public class InboxAdapter extends ArrayAdapter<List<String>> {

    private static final String TAG = "InboxAdapter";

    private View v;

    public InboxAdapter(Context context, int inbox_message_layout, List<List<String>> inbox) {
        super(context, inbox_message_layout, inbox);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        v = convertView;

        if (v == null) {

            v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.inbox_message_layout, parent, false);

        }

        final String friendId = getItem(position).get(PlacesStorage.COMMENTER_POS);
        final String alertText = getItem(position).get(PlacesStorage.ALERT_TEXT_POS);

        final TextView placerMessageView = (TextView) v.findViewById(R.id.placer_card_textView_username);
        final ImageView placerImageView = (ImageView) v.findViewById(R.id.placer_card_profile_pic);

        placerMessageView.setText(alertText);

        Log.d(TAG, "FriendID: " + friendId);

        ParseQuery<PlacesUser> queryUsers = ParseQuery.getQuery("_User");
        queryUsers.whereEqualTo("objectId", friendId);
        queryUsers.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
        queryUsers.getFirstInBackground(new GetCallback<PlacesUser>() {
            @Override
            public void done(final PlacesUser placesUser, ParseException e) {

                if (e == null) {
                    Log.d(TAG, placesUser.getName());

                    PlacesLoginUtils.getInstance().addEntryToUserIdMap(placesUser.getFbId(), placesUser.getName());
                    PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(placesUser.getFbId(), placerImageView, PlacesLoginUtils.PicSize.LARGE, placesUser.getAccountType());

                } else {

                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getContext(), "An error occurred while retrieving Inbox data", Toast.LENGTH_SHORT).show();

                }
            }
        });


        return v;
    }
}
