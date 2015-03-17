package com.gcw.sapienza.places.adapters;

import android.app.Activity;
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
import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by Simone on 3/17/2015.
 */
public class CommentsAdapter extends ArrayAdapter<String> {

    private static final String TAG = "CommentsAdapter";

    private Activity context;

    public CommentsAdapter(Activity context, int resource, List<String> comments) {
        super(context, resource, comments);

        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final View v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.comment_item_layout, parent, false);

        final String commentId = getItem(position);

        final TextView authorView = (TextView) v.findViewById(R.id.author);
        final ImageView authorImageView = (ImageView) v.findViewById(R.id.comment_profile_pic);
        final TextView commentTextView = (TextView) v.findViewById(R.id.comment_text);
        final TextView commentDate = (TextView) v.findViewById(R.id.comment_date);

        ParseQuery<Comment> queryUsers = ParseQuery.getQuery("Comments");
        queryUsers.whereEqualTo("objectId", commentId);
        queryUsers.getFirstInBackground(new GetCallback<Comment>() {
            @Override
            public void done(final Comment comment, ParseException e)
            {
                if (e == null)
                {
                    // TODO Check whether the owner of the comment logged in with fb or g+

                    authorView.setText(comment.getUsername());
                    PlacesLoginUtils.getInstance().addEntryToUserIdMap(comment.getUserId(), comment.getUsername());

                    //since "getFbProfilePictureURL" could last too much
                    //i'm putting an initial std picture in authorImageView
                    PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(comment.getUserId(), authorImageView, PlacesLoginUtils.PicSize.SMALL);
                    commentTextView.setText(comment.getCommentText());

                    DateFormat df = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
                    String date = df.format(comment.getTimestamp());
                    commentDate.setText(date);

                    v.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            ((MainActivity)context).switchToOtherFrag(ProfileFragment.newInstance(comment.getUserId()));
                        }
                    });
                }
                else
                {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getContext(), "An error occurred while retrieving comments' data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }
}
