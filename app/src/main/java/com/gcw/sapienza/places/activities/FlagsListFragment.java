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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.FlagActivity;
import com.gcw.sapienza.places.FlagFragment;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.ShareFragment;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.model.FlagReport;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
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
    private static final String NO_VALID_FLAG_SELECTED = "No valid Flag selected";

    private static final String FLAG_DELETED = "Flag deleted";
    private static final String FLAG_REPORTED = "Flag reported";
    private static final String FLAG_REPORT_REVOKED = "Flag report revoked";

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

        registerForContextMenu(recycleView);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }

    public void updateRecycleViewWithNewContents(List<Flag> l){
        this.recycleView.setAdapter(new FlagsAdapter(l, recycleView, getActivity()));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        FlagsAdapter fa = (FlagsAdapter)recycleView.getAdapter();
        Flag sel_usr = fa.getSelectedFlag();

        if(sel_usr == null) Toast.makeText(getActivity(), NO_VALID_FLAG_SELECTED, Toast.LENGTH_SHORT).show();

        switch (item.getItemId()) {

            case Utils.DELETE_FLAG:
                this.deleteFlag(sel_usr);
                fa.setSelectedFlagIndex(-1);
                return true;

            case Utils.REPORT_FLAG:
                this.reportFlag(sel_usr);
                fa.setSelectedFlagIndex(-1);
                return true;

            case Utils.DELETE_REPORT_FLAG:
                this.deleteReportFlag(sel_usr);
                fa.setSelectedFlagIndex(-1);
                return true;

            default:
                fa.setSelectedFlagIndex(-1);
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Deletes the Flag
     * @param f flag to delete
     */
    private void deleteFlag(Flag f){
        f.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if(e == null) {
                    Toast.makeText(recycleView.getContext(), FLAG_DELETED, Toast.LENGTH_SHORT).show();
                    ((MainActivity2)getActivity()).refresh();
                }else
                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Reports a flag
     * @param f flag to report
     */
    private void reportFlag(final Flag f){

        FlagReport report = FlagReport.createFlagReportFromFlag(f);
        report.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Toast.makeText(recycleView.getContext(), FLAG_REPORTED, Toast.LENGTH_SHORT).show();
                }else
                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Deletes an entry from the Reported_Posts table
     * @param f flag related to the entry to be deleted
     */
    private void deleteReportFlag(Flag f) {
        ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

        queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
        queryDelete.whereEqualTo("reported_flag", f);

        queryDelete.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if(e == null) {
                    for (ParseObject p : parseObjects) {
                        p.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(recycleView.getContext(), FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }else{
                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

class FlagsAdapter extends RecyclerView.Adapter <FlagsAdapter.FlagsViewHolder> {
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
    public void onBindViewHolder(final FlagsViewHolder flagViewHolder, int i) {
        Flag f = this.flags.get(i);

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

        ParseFile pic = f.getPic();
        if(pic != null){
            String url = pic.getUrl();
            Picasso.with(this.view.getContext()).load(url).into(flagViewHolder.main_image);
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

    public static class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener{

        protected final TextView main_text;
        protected final TextView username;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;
        private final Activity mainActivity;

        protected FlagsAdapter flagAdapter;

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

            v.setOnCreateContextMenuListener(this);
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

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
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
                menu.add(0, Utils.DELETE_FLAG, 0, "Delete Flag");
            }else {
                Log.d(TAG, "Username: " + ParseUser.getCurrentUser().getUsername());
                Log.d(TAG, "objectId: " + mFlag.getObjectId());

                ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");
                
                queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
                queryDelete.whereEqualTo("reported_flag", mFlag);

                try {
                    if(queryDelete.count() == 0){
                        menu.add(0, Utils.REPORT_FLAG, 0, "Report Flag as inappropriate");
                    }else{
                        menu.add(0, Utils.DELETE_REPORT_FLAG, 0, "Revoke Flag report");
                    }
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }
            }/*else{
                menu.add(0, Utils.DELETE_REPORT_FLAG, 0, "Revoke Flag report");

            }*/
            //        inflater.inflate(R.menu.context_menu, menu);
        }
    }
}
