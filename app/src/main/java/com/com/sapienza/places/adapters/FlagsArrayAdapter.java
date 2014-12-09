package com.com.sapienza.places.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cgw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.R;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.List;

/**
 * Adapter class for easily showing Posts in a ListView
 */
public class FlagsArrayAdapter extends ArrayAdapter<Flag> {

    public FlagsArrayAdapter(Context context, int layoutResourceId, List<Flag> data){
        super(context, layoutResourceId, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if(row == null){
            //FIXME get the layout inflater from the current context, if the context is not an activity the result is unexpected
            LayoutInflater inflater = ((Activity)this.getContext()).getLayoutInflater();
            //inflate the row from the xml file
            row = inflater.inflate(R.layout.flags_list_item, parent, false);

        }
        //obtain current post
        Flag current_post =  this.getItem(position);

        //obtain post's subviews and configure them
        TextView textView = (TextView)row.findViewById(R.id.flag_list_item_first_line);
        final TextView subtitleTextView = (TextView)row.findViewById(R.id.flag_list_item_subtitle);

        textView.setText(current_post.getText());
        ParseUser post_owner = (ParseUser)current_post.get("user");
        post_owner.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                subtitleTextView.setText((String)object.get("username"));
            }
        });

        return row;
    }
}