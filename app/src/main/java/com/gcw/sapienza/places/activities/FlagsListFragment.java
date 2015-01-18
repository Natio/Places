package com.gcw.sapienza.places.activities;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.List;

/**
 * Created by paolo  on 10/01/15.
 */
public class FlagsListFragment extends Fragment {
    private static final String TAG = "FlagsListFragment";

    private RecyclerView recyvleView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"CIAO");
        View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

        this.recyvleView = (RecyclerView) view.findViewById(R.id.cardList);
        LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        this.recyvleView.setLayoutManager(llm);

        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        Location location = LocationService.getRandomLocation(PlacesApplication.getInstance().getLocation(), 100 );
        ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        query.whereWithinKilometers("location", gp, 10.0f);

        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(List<Flag> flags, ParseException e) {
                if(e != null){
                    Log.d(TAG, e.getMessage());
                }
                else{
                    FlagsListFragment.this.updateRecycleViewWithNewContents(flags);
                }
            }
        });

        return view;
    }





    public void updateRecycleViewWithNewContents(List<Flag> l){
        this.recyvleView.setAdapter(new FlagsAdapter(l, this.getActivity()));
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

        byte[] pic = null;

        try
        {
            if(f.getPic() != null)
            {
                pic = new byte[f.getPic().getData().length];
                System.arraycopy(f.getPic().getData(), 0, pic, 0, pic.length);
            }
        }
        catch(com.parse.ParseException pe)
        {
            Log.v(TAG, "Parse file couldn't be retrieved");
            pe.printStackTrace();
        }

        if(pic != null){
            flagViewHolder.main_image.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.length));
            flagViewHolder.main_image.setVisibility(View.VISIBLE);
        }
        else {
            flagViewHolder.main_image.setImageDrawable(null);
            flagViewHolder.main_image.setVisibility(View.GONE);
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

        return new FlagsViewHolder(itemView);
    }

    public static class FlagsViewHolder extends RecyclerView.ViewHolder{

        protected final TextView main_text;
        protected final TextView username;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;


        public FlagsViewHolder(View v){
            super(v);
            this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
            this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
            this.username = (TextView) v.findViewById(R.id.card_textView_username);
            this.main_text = (TextView) v.findViewById(R.id.card_textView_text);
        }
    }
}
