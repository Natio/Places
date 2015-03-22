package com.gcw.sapienza.places.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.fragments.FlagFragment;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.PlacesUtilCallback;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.google.gson.Gson;
import com.parse.FindCallback;
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
public class FlagsAdapter extends RecyclerView.Adapter<FlagsAdapter.FlagsViewHolder> {

    private static final String TAG = "FlagsAdapter";

    private final List<Flag> flags;
    private final View view;
    private final Activity mainActivity;
    private final Transformation transformation = new CropCircleTransformation();


    private int selectedFlagIndex;

    public FlagsAdapter(List<Flag> list, View v, Activity mainActivity) {
        this.flags = list;
        this.view = v;
        this.mainActivity = mainActivity;
    }

    public Flag getSelectedFlag() {
        try {
            return flags.get(selectedFlagIndex);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public void setSelectedFlagIndex(int i) {
        selectedFlagIndex = i;
    }

    @Override
    public void onBindViewHolder(final FlagsAdapter.FlagsViewHolder flagViewHolder, final int i) {
        Flag f = this.flags.get(i);
        flagViewHolder.setCurrentFlag(f);
        flagViewHolder.flagAdapter = this;

        if (f.getPassword() == null) {
            String text = f.getText();
            if (text != null && text.length() > 0) {
                flagViewHolder.main_text.setVisibility(View.VISIBLE);
                flagViewHolder.main_text.setText(f.getText());
            } else {
                flagViewHolder.main_text.setVisibility(View.GONE);
            }


        } else flagViewHolder.main_text.setText("***Private flag***");


        String user_id = f.getFbId();
        String account_type = f.getAccountType();
        String fb_username = f.getFbName(); // checks if Flag has fb username. if there is one use it otherwise ask FB

        // FIXME If user is logged in with G+, FB Graph API cannot be used
        if (fb_username == null) PlacesLoginUtils.getInstance().loadUsernameIntoTextView(user_id, flagViewHolder.username, f.getAccountType());
        else flagViewHolder.username.setText(fb_username);


        PlacesLoginUtils.getInstance().getProfilePictureURL(user_id, account_type, PlacesLoginUtils.PicSize.SMALL, new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e)
            {
                Log.d(TAG, "Pic path: " + result);

                if(result != null && !result.isEmpty())
                {
                    Log.d(TAG, "Inserting profile pic in card " + i);

                    Picasso.with(FlagsAdapter.this.view.getContext()).load(result).transform(transformation).into(flagViewHolder.user_profile_pic);
                }
            }
        });

        int numberOfWows= f.getWowCount();
        flagViewHolder.stats_wow.setText(numberOfWows+" WoW");


        int numberOfComments = f.getNumberOfComments();
        if(numberOfComments == 1){
            flagViewHolder.stats_comment.setText(numberOfComments + " comment");
        }
        else{
            flagViewHolder.stats_comment.setText(numberOfComments + " comments");
        }
/*
        ParseQuery<Comment> countQuery = ParseQuery.getQuery("Comments");
        countQuery.whereEqualTo("flagId", f.getFlagId());
        countQuery.countInBackground(new CountCallback() {
            @Override
            public void done(int i, ParseException e)
            {
                if(i == 1) flagViewHolder.stats_comment.setText(i + " comment");
                else flagViewHolder.stats_comment.setText(i + " comments");
            }
        });
*/

        ParseFile pic = f.getThumbnail();
        if (pic != null && f.getPassword() == null) {
            String url = pic.getUrl();
            flagViewHolder.main_image.setVisibility(View.VISIBLE);
            flagViewHolder.main_image.setClickable(false);
            Picasso.with(this.view.getContext()).load(url).fit().centerCrop().into(flagViewHolder.main_image);

        } else {
            flagViewHolder.main_image.setVisibility(View.GONE);
            flagViewHolder.main_image.setImageDrawable(null);
        }


        ((CardView) flagViewHolder.itemView).setShadowPadding(0, 0, 7, 7);

        //working on making icons or symbols which represents a category

        String[] category_array = PlacesApplication.getInstance().getResources().getStringArray(R.array.categories);

        if (flagViewHolder.mFlag.getCategory().equals(category_array[0])) { //None
            flagViewHolder.categoryIcon.setImageResource(R.drawable.none);
        } else if (flagViewHolder.mFlag.getCategory().equals(category_array[1])) { //Thoughts
            flagViewHolder.categoryIcon.setImageResource(R.drawable.thoughts);
        } else if (flagViewHolder.mFlag.getCategory().equals(category_array[2])) { //Fun
            flagViewHolder.categoryIcon.setImageResource(R.drawable.smile);
        } else if (flagViewHolder.mFlag.getCategory().equals(category_array[3])) { //Music
            flagViewHolder.categoryIcon.setImageResource(R.drawable.music);
        } else if (flagViewHolder.mFlag.getCategory().equals(category_array[4])) { //Landscape
            flagViewHolder.categoryIcon.setImageResource(R.drawable.eyes);
        } else {
            flagViewHolder.categoryIcon.setImageResource(R.drawable.food);
        }

        //************************

        /*
        String[] category_array = PlacesApplication.getInstance().getResources().getStringArray(R.array.categories);
        if (flagViewHolder.mFlag.getCategory().equals(category_array[0])){ //None
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 255, 0, 0));
        }else if (flagViewHolder.mFlag.getCategory().equals(category_array[1])){ //Thoughts
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 0, 255, 0));
        }else if (flagViewHolder.mFlag.getCategory().equals(category_array[2])){ //Fun
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 255, 255, 0));
        }else if (flagViewHolder.mFlag.getCategory().equals(category_array[3])){ //Music
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 0, 0, 255));
        }else if (flagViewHolder.mFlag.getCategory().equals(category_array[4])){ //Landscape
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 204, 204, 204));
        }else{ //Food
            ((CardView)flagViewHolder.itemView).setCardBackgroundColor(Color.argb(20, 128, 0, 128));
        }
        */
    }

    @Override
    public int getItemCount() {
        if (this.flags == null) return 0;
        else return this.flags.size();
    }

    @Override
    public int getItemViewType(int position) {
        // This is brilliant, I can explain
        //return position;

        //This must be the same value for each view otherwise the RecyclerView will perform badly.
        return 0;
    }

    @Override
    public FlagsAdapter.FlagsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new FlagsAdapter.FlagsViewHolder(itemView, mainActivity);
    }

    public static class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

        protected final TextView main_text;
        protected final TextView username;
        protected final TextView stats_wow;
        protected final TextView stats_comment;
        protected final ImageView user_profile_pic;
        protected final ImageView main_image;
        private final Activity mainActivity;

        //new view for icon or line representing the category
        protected ImageView categoryIcon;
        protected FlagsAdapter flagAdapter;
        private String password;
        private Flag mFlag;
        private int numberOfWows;

        public FlagsViewHolder(View v, Activity context) {
            super(v);

            this.mainActivity = context;

            v.setOnClickListener(this);

            this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
            this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
            this.username = (TextView) v.findViewById(R.id.card_textView_username);
            this.main_text = (TextView) v.findViewById(R.id.card_textView_text);
            this.stats_wow = (TextView) v.findViewById(R.id.stats_wows);
            this.stats_comment = (TextView) v.findViewById(R.id.stats_comments);

            //new for icon representing category, or a colored line
            this.categoryIcon = (ImageView) v.findViewById(R.id.categoryIcon);

            v.setOnCreateContextMenuListener(this);
        }

        public void setCurrentFlag(Flag flag) {
            this.mFlag = flag;
        }

        @Override
        public void onClick(View v) {
            if (mFlag.getPassword() != null) askForPassword(mFlag.getPassword());
            else openFlag();
        }

        private void openFlag()
        {
            Date date = mFlag.getDate();

            DateFormat df = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
            String sDate = df.format(date);

            Bundle bundle = new Bundle();

            bundle.putString("text", mFlag.getText());
            bundle.putString("id", mFlag.getFbId());
            bundle.putString("date", sDate);
            bundle.putString("weather", mFlag.getWeather());
            bundle.putString("category", mFlag.getCategory());
            bundle.putBoolean("inPlace", mFlag.getInPlace());
            bundle.putString("flagId", mFlag.getFlagId());
            bundle.putString("author", mFlag.getFbName());

            bundle.putInt("wowCount", mFlag.getWowCount());
            bundle.putInt("lolCount", mFlag.getLolCount());
            bundle.putInt("booCount", mFlag.getBooCount());

            //FIXME investigate why serialization works only on selected devices
//            bundle.putString("flag", new Gson().toJson(mFlag));

            bundle.putString("accountType", mFlag.getAccountType());

            ParseFile file;
            FlagFragment.MediaType mediaType = FlagFragment.MediaType.NONE;
            if ((file = this.mFlag.getPic()) != null) {
                mediaType = FlagFragment.MediaType.PIC;
            } else if ((file = this.mFlag.getVideo()) != null) {
                mediaType = FlagFragment.MediaType.VIDEO;
            } else if ((file = this.mFlag.getAudio()) != null) {
                mediaType = FlagFragment.MediaType.AUDIO;
            }

            FlagFragment frag = new FlagFragment();
            frag.setMedia(file, mediaType);
            frag.setArguments(bundle);

            ((MainActivity) mainActivity).switchToOtherFrag(frag);
        }

        private void askForPassword(final String psw) {
            LayoutInflater li = LayoutInflater.from(mainActivity);
            View passwordDialogLayout = li.inflate(R.layout.password_dialog, null);
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
            alertDialogBuilder.setView(passwordDialogLayout);

            final EditText userInput = (EditText) passwordDialogLayout.findViewById(R.id.password_field);

            // final InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

            alertDialogBuilder
                    .setCancelable(true)
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            })
                    .setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    password = userInput.getText().toString();
                                    if (!password.equals(psw)) {
                                        Toast.makeText(mainActivity, "Wrong password", Toast.LENGTH_LONG).show();
                                        userInput.setText("");
                                    } else {
                                        dialog.dismiss();

                                        openFlag();
                                    }
                                }

                            }

                    );

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            // inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            // inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
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
            if (PlacesLoginUtils.getInstance().getCurrentUserId().equals(fb_id)) {
                menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_FLAG, 0, "Delete Flag");
            } else {
                Log.d(TAG, "Username: " + ParseUser.getCurrentUser().getUsername());
                Log.d(TAG, "objectId: " + mFlag.getObjectId());

                ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

                queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
                queryDelete.whereEqualTo("reported_flag", mFlag);

                try {
                    if (queryDelete.count() == 0) {
                        menu.add(Utils.FLAG_LIST_GROUP, Utils.REPORT_FLAG, 0, "Report Flag as inappropriate");
                    } else {
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

        //this is as updateWowInfo in the FlagFragment, but only for wow and used for counter in the
        //card view, useful when we will have a button instead of a textView
        private void updateWowCounter() { //like update
            ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
            queryPosts.whereEqualTo("objectId", mFlag.getFlagId());

            queryPosts.findInBackground(new FindCallback<Flag>() {
                @Override
                public void done(List<Flag> markers, ParseException e) {
                    if (e == null && markers.size() != 0) {
                        Flag flag = markers.get(0);

                        numberOfWows = flag.getInt("wowCount");
                    }
                }
            });

            stats_wow.setText(numberOfWows + " WoW");


            // TODO I totally have to go over this mess
            //for now only number of wows is shown, we could like it also from cardView with a button
            /*
            ParseQuery<CustomParseObject> queryW = ParseQuery.getQuery("Wow_Lol_Boo");
            queryW.whereEqualTo("fbId", userId);
            queryW.whereEqualTo("flagId", flagId);
            queryW.whereEqualTo("boolWow", true);

            queryW.findInBackground(new FindCallback<CustomParseObject>() {
                public void done(List<CustomParseObject> markers, ParseException e) {
                    if (e == null && markers.size() != 0) {
                        numberOfWows--; //me and all the others but me
                        stats_wow.setText("You and other "+ numberOfWows+ " WoWed this");
                    }
                }
            });
            */

    /*
            ParseQuery<CustomParseObject> queryL = ParseQuery.getQuery("Wow_Lol_Boo");
            queryL.whereEqualTo("fbId", userId);
            queryL.whereEqualTo("flagId", flagId);
            queryL.whereEqualTo("boolLol", true);

            queryL.findInBackground(new FindCallback<CustomParseObject>() {
                public void done(List<CustomParseObject> markers, ParseException e) {
                    if (e == null && markers.size() != 0) {
                        lolButton.setText("You lol this." + " (" + lolCount + ")");
                    }
                }
            });

            ParseQuery<CustomParseObject> queryB = ParseQuery.getQuery("Wow_Lol_Boo");
            queryB.whereEqualTo("fbId", userId);
            queryB.whereEqualTo("flagId", flagId);
            queryB.whereEqualTo("boolBoo", true);

            queryB.findInBackground(new FindCallback<CustomParseObject>() {
                public void done(List<CustomParseObject> markers, ParseException e) {
                    if (e == null && markers.size() != 0) {
                        booButton.setText("You boo this." + " (" + booCount + ")");
                    }
                }
            });
            */
        }


    }

}
