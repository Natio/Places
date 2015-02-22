package com.gcw.sapienza.places.adapters;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.fragments.FlagFragment;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by paolo on 18/02/15.
 */
public class FlagsAdapter extends RecyclerView.Adapter <FlagsAdapter.FlagsViewHolder> {
    private static final String TAG = "FlagsAdapter";
    private final List<Flag> flags;
    private final View view;
    private final Activity mainActivity;
    private final Transformation transformation = new CropCircleTransformation();

    private int selectedFlagIndex;

    public FlagsAdapter(List<Flag> list, View v, Activity mainActivity){
        this.flags = list;
        this.view = v;
        this.mainActivity = mainActivity;
    }

    public Flag getSelectedFlag() {
        try{
            return flags.get(selectedFlagIndex);
        }
        catch (IndexOutOfBoundsException e){
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
    public void setSelectedFlagIndex(int i){ selectedFlagIndex = i;}

    @Override
    public void onBindViewHolder(final FlagsAdapter.FlagsViewHolder flagViewHolder, int i) {
        Flag f = this.flags.get(i);
        flagViewHolder.setCurrentFlag(f);
        flagViewHolder.flagAdapter = this;

        flagViewHolder.main_text.setText(f.getText());

        String user_id = f.getFbId();

        String fb_username = f.getFbName(); // checks if Flag has fb username. if there is one use it otherwise ask FB
        if(fb_username == null){
            FacebookUtils.getInstance().loadUsernameIntoTextView(user_id, flagViewHolder.username);

        }
        else{
            flagViewHolder.username.setText(fb_username);
        }

        FacebookUtils.getInstance().getFbProfilePictureURL(user_id, FacebookUtils.PicSize.SMALL, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                Picasso.with(FlagsAdapter.this.view.getContext()).load(result).transform(transformation).into(flagViewHolder.user_profile_pic);
            }
        });

        ParseFile pic = f.getThumbnail();
        if(pic != null){
            String url = pic.getUrl();
            Picasso.with(this.view.getContext()).load(url).into(flagViewHolder.main_image);
            flagViewHolder.main_image.setVisibility(View.VISIBLE);
            flagViewHolder.main_image.setClickable(false);
        }
        else{
            flagViewHolder.main_image.setVisibility(View.GONE);
            flagViewHolder.main_image.setImageDrawable(null);
        }

    }

    @Override
    public int getItemCount()
    {
        if(this.flags == null) return 0;
        else return this.flags.size();
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
    public FlagsAdapter.FlagsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new FlagsAdapter.FlagsViewHolder(itemView, mainActivity);
    }

    public static class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener{

        protected final TextView main_text;
        protected final TextView username;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;
        private final Activity mainActivity;

        protected FlagsAdapter flagAdapter;

        private Flag mFlag;

        public void setCurrentFlag(Flag flag){
            this.mFlag = flag;
        }


        public FlagsViewHolder(View v,  Activity context)
        {
            super(v);

            this.mainActivity = context;

            v.setOnClickListener(this);

            this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
            this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
            this.username = (TextView) v.findViewById(R.id.card_textView_username);
            this.main_text = (TextView) v.findViewById(R.id.card_textView_text);

            v.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v)
        {

            Date date = mFlag.getDate();
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());
            String sDate = df.format(date);

            String[] date_time = sDate.split(" ");

            sDate = "\nDate: " + date_time[0] + "\nTime: " + date_time[1];

            Bundle bundle = new Bundle();

            bundle.putString("text", mFlag.getText());
            bundle.putString("id", mFlag.getFbId());
            bundle.putString("date", sDate);
            bundle.putString("weather", mFlag.getWeather());
            bundle.putString("category", mFlag.getCategory());
            bundle.putBoolean("inPlace", mFlag.getInPlace());
            bundle.putString("flagId", mFlag.getFlagId());
            bundle.putInt("wowCount", mFlag.getWowCount());


            ParseFile file;
            FlagFragment.MediaType mediaType = FlagFragment.MediaType.NONE;
            if((file = this.mFlag.getPic()) != null){
                mediaType = FlagFragment.MediaType.PIC;
            }
            else if((file = this.mFlag.getVideo()) != null){
                mediaType = FlagFragment.MediaType.VIDEO;
            }
            else if((file = this.mFlag.getAudio()) != null){
                mediaType = FlagFragment.MediaType.AUDIO;
            }

            FlagFragment frag = new FlagFragment();
            frag.setMedia(file, mediaType);
            frag.setArguments(bundle);

            ((MainActivity)mainActivity).switchToOtherFrag(frag);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//            flagsAdapter.setSelectedFlagIndex(getPosition());
            menu.setHeaderTitle("Edit");
            flagAdapter.setSelectedFlagIndex(getPosition());
            Log.d(TAG, "Item position: " + getPosition());
            String fb_id = mFlag.getFbId();
            //        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            //        Flag sel_usr = (Flag)(recycleView.getItemAtPosition(info.position));
            //        String fb_id = sel_usr.getFbId();
            //
            if(FacebookUtils.getInstance().getCurrentUserId().equals(fb_id)) {
                menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_FLAG, 0, "Delete Flag");
            }else {
                Log.d(TAG, "Username: " + ParseUser.getCurrentUser().getUsername());
                Log.d(TAG, "objectId: " + mFlag.getObjectId());

                ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

                queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
                queryDelete.whereEqualTo("reported_flag", mFlag);

                try {
                    if(queryDelete.count() == 0){
                        menu.add(Utils.FLAG_LIST_GROUP, Utils.REPORT_FLAG, 0, "Report Flag as inappropriate");
                    }else{
                        menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_REPORT_FLAG, 0, "Revoke Flag report");
                    }
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }
            }/*else{
                menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_REPORT_FLAG, 0, "Revoke Flag report");

            }*/
            //        inflater.inflate(R.menu.context_menu, menu);
        }
    }
}
