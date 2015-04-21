package com.gcw.sapienza.places.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesUtilCallback;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Simone on 3/17/2015.
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder> {

    private static final String TAG = "CommentsAdapter";

    private Activity mainActivity;
    private List<Comment> comments;
    private View view;

    private int selectedCommentIndex;

    public CommentsAdapter(List<Comment> list, View v, Activity mainActivity)
    {
        this.comments = list;
        this.view = v;
        this.mainActivity = mainActivity;
    }

    public Comment getSelectedComment()
    {
        try {
            return comments.get(selectedCommentIndex);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public void setSelectedCommentIndex(int i) {
        selectedCommentIndex = i;
    }

    @Override
    public void onBindViewHolder(final CommentsAdapter.CommentsViewHolder commentViewHolder, final int i)
    {
        Comment f = this.comments.get(i);
        commentViewHolder.setCurrentComment(f);
        commentViewHolder.commentAdapter = this;

        String user_id = f.getUserId();
        String account_type = f.getAccountType();
        String fb_username = f.getUsername(); // checks if Flag has fb username. if there is one use it otherwise ask FB

        // If user is logged in with G+, FB Graph API cannot be used
        if (fb_username == null)
            PlacesLoginUtils.getInstance().loadUsernameIntoTextView(user_id, commentViewHolder.username, f.getAccountType());
        else
        {
            if(!PlacesLoginUtils.getInstance().getUserIdMap().containsKey(user_id)) PlacesLoginUtils.getInstance().addEntryToUserIdMap(user_id, fb_username);
            commentViewHolder.username.setText(fb_username);
        }


        PlacesLoginUtils.getInstance().getProfilePictureURL(user_id, account_type, PlacesLoginUtils.PicSize.SMALL, new PlacesUtilCallback()
        {
            @Override
            public void onResult(String result, Exception e)
            {
                if (result != null && !result.isEmpty())
                {
                    Picasso.with(view.getContext()).load(result).into(commentViewHolder.profile_pic);
                }
            }
        });

        Date date = f.getTimestamp();
        DateFormat df = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
        String sDate = df.format(date);
        commentViewHolder.comment_date.setText(sDate);

        commentViewHolder.comment_text.setText(f.getCommentText());

        // ((CardView) commentViewHolder.itemView).setShadowPadding(0, 0, 7, 7);
    }

    @Override
    public int getItemCount()
    {
        if (this.comments == null) return 0;
        else return this.comments.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        // This is brilliant, I can explain
        //return position;

        //This must be the same value for each view otherwise the RecyclerView will perform badly.
        return 0;
    }

    @Override
    public CommentsAdapter.CommentsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.comment_item_layout, viewGroup, false);

        return new CommentsAdapter.CommentsViewHolder(itemView);
    }

    public static class CommentsViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
    {
        protected final TextView comment_text;
        protected final TextView username;
        protected final TextView comment_date;
        protected final ImageView profile_pic;

        protected CommentsAdapter commentAdapter;
        private Comment mComment;

        public CommentsViewHolder(View v)
        {
            super(v);

            this.profile_pic = (ImageView) v.findViewById(R.id.comment_profile_pic);
            this.username = (TextView) v.findViewById(R.id.author);
            this.comment_text = (TextView) v.findViewById(R.id.comment_text);
            this.comment_date = (TextView) v.findViewById(R.id.comment_date);

            v.setOnCreateContextMenuListener(this);
        }

        public void setCurrentComment(Comment comment) {
            this.mComment = comment;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
        {
            menu.setHeaderTitle("Edit");
            commentAdapter.setSelectedCommentIndex(getPosition());

            String fb_id = mComment.getUserId();

            if (PlacesLoginUtils.getInstance().getCurrentUserId().equals(fb_id)) {
                menu.add(PlacesUtils.COMMENT_LIST_GROUP, PlacesUtils.DELETE_COMMENT, 0, "Delete Flag");
            }
            else
            {
                ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

                queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
                queryDelete.whereEqualTo("reported_flag", mComment);

                try {
                    if (queryDelete.count() == 0) {
                        menu.add(PlacesUtils.COMMENT_LIST_GROUP, PlacesUtils.REPORT_COMMENT, 0, "Report Flag as inappropriate");
                    } else {
                        menu.add(PlacesUtils.COMMENT_LIST_GROUP, PlacesUtils.DELETE_REPORT_COMMENT, 0, "Revoke Flag report");
                    }
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}