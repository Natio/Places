package com.gcw.sapienza.places.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.FlagActivity;
import com.gcw.sapienza.places.FlagFragment;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.ShareFragment;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.ParseFile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by paolo  on 10/01/15.
 */
public class FlagsListFragment extends Fragment {

    private static final String TAG = "FlagsListFragment";

    private RecyclerView recycleView;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(LocationService.FOUND_NEW_FLAGS_NOTIFICATION)){
                FlagsListFragment.this.updateRecycleViewWithNewContents(PlacesApplication.getInstance().getFlags());

            }
            Log.d(TAG, intent.getAction());
        }
    };

    public RecyclerView getRV()
    {
        return recycleView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

        this.recycleView = (RecyclerView) view.findViewById(R.id.cardList);
        LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        this.recycleView.setLayoutManager(llm);

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));

        this.updateRecycleViewWithNewContents(PlacesApplication.getInstance().getFlags());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }

    public void updateRecycleViewWithNewContents(List<Flag> l){
        this.recycleView.setAdapter(new FlagsAdapter(l, this.getActivity()));
    }
}

class FlagsAdapter extends RecyclerView.Adapter <FlagsAdapter.FlagsViewHolder>{
    private static final String TAG = "FlagsAdapter";
    private final List<Flag> flags;
    private final Activity mainActivity;
    private final Transformation transformation = new CropCircleTransformation();

    public FlagsAdapter(List<Flag> list, Activity ctx){
        this.flags = list;
        this.mainActivity = ctx;
    }


    @Override
    public void onBindViewHolder(final FlagsViewHolder flagViewHolder, int i) {
        Flag f = this.flags.get(i);
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
                Picasso.with(FlagsAdapter.this.mainActivity).load(result).transform(transformation).into(flagViewHolder.user_profile_pic);
            }
        });

        ParseFile pic = f.getPic();
        if(pic != null){
            String url = pic.getUrl();
            Picasso.with(this.mainActivity).load(url).into(flagViewHolder.main_image);
            flagViewHolder.main_image.setVisibility(View.VISIBLE);
        }
        else{
            flagViewHolder.main_image.setVisibility(View.GONE);
            flagViewHolder.main_image.setImageDrawable(null);
        }

    }

    @Override
    public int getItemCount() {
        return this.flags.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        // This is brilliant, I can explain
        return position;
    }

    @Override
    public FlagsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new FlagsViewHolder(itemView, this.flags.get(i), mainActivity);
    }

    public static class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        protected final TextView main_text;
        protected final TextView username;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;

        private final Activity mainActivity;

        private Flag mFlag;

        public FlagsViewHolder(View v, Flag flag, Activity context)
        {
            super(v);

            this.mFlag = flag;
            this.mainActivity = context;

            v.setOnClickListener(this);

            this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
            this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
            this.username = (TextView) v.findViewById(R.id.card_textView_username);
            this.main_text = (TextView) v.findViewById(R.id.card_textView_text);
        }

        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(mainActivity, FlagActivity.class);

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

            try
            {
                ParseFile pic_file;
                if((pic_file = mFlag.getPic()) != null){
                    File temp = File.createTempFile("places_temp_pic", ShareFragment.PICTURE_FORMAT, mainActivity.getCacheDir());
                    temp.deleteOnExit();

                    FileOutputStream outStream = new FileOutputStream(temp);
                    outStream.write(pic_file.getData());
                    outStream.close();

                    bundle.putString("picture", temp.getAbsolutePath());
                }

                ParseFile audio_file;
                if((audio_file = mFlag.getAudio()) != null){
                    File temp = File.createTempFile("places_temp_audio", ShareFragment.AUDIO_FORMAT, mainActivity.getCacheDir());
                    temp.deleteOnExit();

                    FileOutputStream outStream = new FileOutputStream(temp);
                    outStream.write(audio_file.getData());
                    outStream.close();
                    bundle.putString("audio", temp.getAbsolutePath());
                }


                ParseFile video_file;
                if((video_file = mFlag.getVideo()) != null)
                {
                    File temp = File.createTempFile("places_temp_video", ShareFragment.VIDEO_FORMAT, mainActivity.getCacheDir());
                    temp.deleteOnExit();

                    FileOutputStream outStream = new FileOutputStream(temp);
                    outStream.write(video_file.getData());
                    outStream.close();

                    bundle.putString("video", temp.getAbsolutePath());
                }
            }
            catch(IOException ioe){ioe.printStackTrace();}
            catch(com.parse.ParseException pe)
            {
                Log.v(TAG, "Parse file(s) couldn't be retrieved");
                pe.printStackTrace();
            }

            // intent.putExtras(bundle);

            // mContext.startActivity(intent);

            FlagFragment frag = new FlagFragment();
            frag.setArguments(bundle);

            ((MainActivity2)mainActivity).switchToFlagFrag(frag);
        }
    }
}
