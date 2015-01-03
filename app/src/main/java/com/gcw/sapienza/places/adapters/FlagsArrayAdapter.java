package com.gcw.sapienza.places.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Adapter class for easily showing Posts in a ListView
 */
public class FlagsArrayAdapter extends ArrayAdapter<Flag> {

    private static final String TAG = "FlagsArrayAdapter";
    private Activity activity;

    public FlagsArrayAdapter(Context context, int layoutResourceId, List<Flag> data, Activity activity){
        super(context, layoutResourceId, data);

        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)this.getContext()).getLayoutInflater();
            //inflate the row from the xml file
            row = inflater.inflate(R.layout.new_flags_list_item, parent, false);
        }

        final View final_row = row;

        //obtain current post
        final Flag current_post =  this.getItem(position);

        //obtain post's subviews and configure them
        TextView textView = (TextView)row.findViewById(R.id.flag_list_item_first_line);
        // final TextView subtitleTextView = (TextView)row.findViewById(R.id.flag_list_item_subtitle);

        textView.setText(current_post.getText());

        final String fbId = current_post.getFbId();

        /*
        if(!Utils.userIdMap.containsKey(fbId))
        {
            Utils.fetchFbUsername(fbId);

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userIdMap.containsKey(fbId)) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else subtitleTextView.setText(Utils.userIdMap.get(fbId));
                }
            });
        }
        else subtitleTextView.setText(Utils.userIdMap.get(fbId));
        */

        if(!Utils.userProfilePicMapSmall.containsKey(fbId))
        {
            try
            {
                Utils.fetchFbProfilePic(fbId, Utils.SMALL_PIC_SIZE);
            }
            catch(MalformedURLException mue){ mue.printStackTrace(); }
            catch (IOException ioe){ ioe.printStackTrace(); }

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userProfilePicMapSmall.containsKey(fbId)) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else streamProfilePicToAdapter(final_row, fbId);
                }
            });
        }
        else streamProfilePicToAdapter(row, fbId);
  
        return row;
    }

    protected void streamProfilePicToAdapter(final View row, final String fbId)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(Utils.userProfilePicMapSmall.get(fbId)).openConnection().getInputStream());

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) row.findViewById(R.id.flag_list_item_pic)).setImageBitmap(bitmap);
                        }
                    });
                }
                catch(MalformedURLException mue){ mue.printStackTrace(); }
                catch (IOException ioe){ ioe.printStackTrace(); }
            }
        }).start();
    }
}
