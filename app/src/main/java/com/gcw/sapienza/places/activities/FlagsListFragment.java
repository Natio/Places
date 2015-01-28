package com.gcw.sapienza.places.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.location.Location;
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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.ParseFile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.List;

/**
 * Created by paolo  on 10/01/15.
 */
public class FlagsListFragment extends Fragment {

    private static final String TAG = "FlagsListFragment";

    private RecyclerView recyvleView;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(LocationService.FOUND_NEW_FLAGS_NOTIFICATION)){
                FlagsListFragment.this.updateRecycleViewWithNewContents(PlacesApplication.getInstance().getFlags());

            }
            Log.d(TAG, intent.getAction());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

        this.recyvleView = (RecyclerView) view.findViewById(R.id.cardList);
        LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        this.recyvleView.setLayoutManager(llm);

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
        this.recyvleView.setAdapter(new FlagsAdapter(l, this.getActivity()));
        Log.d(TAG, l.size() + "");
    }
}

class FlagsAdapter extends RecyclerView.Adapter <FlagsAdapter.FlagsViewHolder>{
    private static final String TAG = "FlagsAdapter";
    private final List<Flag> flags;
    private final Context context;
    private final Transformation transformation = new CropCircleTransformation();

    public FlagsAdapter(List<Flag> list, Context ctx){
        this.flags = list;
        this.context = ctx;
    }


    @Override
    public void onBindViewHolder(final FlagsViewHolder flagViewHolder, int i) {
        Flag f = this.flags.get(i);
        flagViewHolder.main_text.setText(f.getText());

        String user_id = f.getFbId();

        FacebookUtils.getInstance().loadUsernameIntoTextView(user_id, flagViewHolder.username);
        FacebookUtils.getInstance().getFbProfilePictureURL(user_id, FacebookUtils.PicSize.SMALL, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                Picasso.with(FlagsAdapter.this.context).load(result).transform(transformation).into(flagViewHolder.user_profile_pic);
            }
        });

        ParseFile pic = f.getPic();
        if(pic != null){
            String url = pic.getUrl();
            Picasso.with(this.context).load(url).into(flagViewHolder.main_image);
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
    public FlagsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new FlagsViewHolder(itemView, context);
    }

    public static class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        protected final TextView main_text;
        protected final TextView username;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;

        private final Context mContext;

        public FlagsViewHolder(View v, Context context)
        {
            super(v);

            this.mContext = context;

            v.setOnClickListener(this);

            this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
            this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
            this.username = (TextView) v.findViewById(R.id.card_textView_username);
            this.main_text = (TextView) v.findViewById(R.id.card_textView_text);
        }

        @Override
        public void onClick(View v)
        {
            // TODO start FlagActivity!
            Toast.makeText(mContext, "FlagActivity integration still to be implemented!", Toast.LENGTH_SHORT).show();
        }
    }
}
