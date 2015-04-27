/*
 * Copyright 2015-present Places®.
 */
package com.gcw.sapienza.places.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.activities.ShareActivity;
import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.CommentReport;
import com.gcw.sapienza.places.models.CustomParseObject;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.models.manager.CommentsManager;
import com.gcw.sapienza.places.models.manager.ModelCallback;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesUtilCallback;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/*
 * This class represents the fragment that contains info regarding a flag like author,
 * weather, text, comments, wow, and the interface overall behavior(ex.: media management)
 */

public class FlagFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private static final String TAG = "FlagFragment";
    private static final int WOW_CODE = 0;
    private String newComment;
    private boolean scrollToLastComment = false;
    private MediaPlayer mediaPlayer;
    private VideoView videoView;
    private ImageView imageView;
    private ImageView bigView;
    private ImageView playVideoButton;
    private ImageView audioHolder;
    private TextView wowStatText;
    private RelativeLayout frameLayout;
    private LinearLayout wholeFlagContainer;
    private LinearLayout audioLayout;
    private LinearLayout imageHolder;
    private FrameLayout videoHolder;
    private TableLayout commentsView;
    private View lastCommentPos;
    private ToggleButton newWowButton;

    private MediaType mediaType;
    private ParseFile mediaFile;
    private Flag flag;

    /*
     * Must be called BEFORE adding the fragment to the
     *
     * @param mediaFile the file to display
     * @param type      the type of the file
     */
    private void setMedia(ParseFile mediaFile, MediaType type) {
        this.mediaType = type;
        this.mediaFile = mediaFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            this.scrollToLastComment = bundle.getBoolean("scrollToLastComment");
        }
    }

    /**
     * @param scrollToLastComment if true the list of comment get scrolled to the end
     */
    public void setScrollToLastComment(boolean scrollToLastComment) {
        this.scrollToLastComment = scrollToLastComment;
    }

    public void setFlag(Flag f) {
        if (this.flag != null) {
            throw new RuntimeException("");
        }
        this.flag = f;
        ParseFile file;
        FlagFragment.MediaType mediaType = FlagFragment.MediaType.NONE;
        if ((file = this.flag.getPic()) != null) {
            mediaType = FlagFragment.MediaType.PIC;
        } else if ((file = this.flag.getVideo()) != null) {
            mediaType = FlagFragment.MediaType.VIDEO;
        } else if ((file = this.flag.getAudio()) != null) {
            mediaType = FlagFragment.MediaType.AUDIO;
        }
        this.setMedia(file, mediaType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.flag_layout, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        // commentsRecyclerView = (RecyclerView) view.findViewById(R.id.comments_holder);
        // commentsRecyclerView.setLayoutManager(llm);

        TextView flagText = (TextView) view.findViewById(R.id.text);
        TextView authorTextView = (TextView) view.findViewById(R.id.author);
        TextView dateTextView = (TextView) view.findViewById(R.id.dateInfo);
        TextView temperatureView = (TextView) view.findViewById(R.id.temperature);
        ImageView categoryIco = (ImageView) view.findViewById(R.id.categoryIcon);
        ImageView weatherIco = (ImageView) view.findViewById(R.id.meteo);
        ImageView profilePicImageView = (ImageView) view.findViewById(R.id.profile_pic);
        lastCommentPos = view.findViewById(R.id.last_comment_pos);

        String weatherInfo = this.flag.getWeather();
        String temperature;
        String weather;

        //temporary quick solution to manage openweathermap when didn't work, and make app working all the same
        if (weatherInfo == null || weatherInfo.isEmpty()) {
            temperature = "" + 20 + "°C";
            weather = "Cloud";
        } else {
            StringTokenizer st = new StringTokenizer(weatherInfo, ",");
            temperature = st.nextToken();
            weather = st.nextToken().substring(1);
        }

        temperatureView.setText(temperature);

        if (weather.equals("Rain")) {
            weatherIco.setImageResource(R.drawable.rain);
        } else if (weather.equals("Clouds")) {
            weatherIco.setImageResource(R.drawable.cloud);
        } else if (weather.equals("Clear")) {
            weatherIco.setImageResource(R.drawable.sun);
        } else if (weather.equals("Snow")) {
            weatherIco.setImageResource(R.drawable.snow);
        } else
            weatherIco.setImageResource(R.drawable.cloudsun);

        String[] category_array = PlacesApplication.getInstance().getResources().getStringArray(R.array.categories);
        String category = this.flag.getCategory();
        if (category.equals(category_array[0])) { //None
            categoryIco.setImageResource(R.drawable.none);
        } else if (category.equals(category_array[1])) { //Thoughts
            categoryIco.setImageResource(R.drawable.thoughts);
        } else if (category.equals(category_array[2])) { //Fun
            categoryIco.setImageResource(R.drawable.smile);
        } else if (category.equals(category_array[3])) { //Music
            categoryIco.setImageResource(R.drawable.music);
        } else if (category.equals(category_array[4])) { //Landscape
            categoryIco.setImageResource(R.drawable.eyes);
        } else {
            categoryIco.setImageResource(R.drawable.food);
        }

        frameLayout = (RelativeLayout) view.findViewById(R.id.frame_layout);

        //to show big preview
        bigView = (ImageView) view.findViewById(R.id.pic_big_preview);

        wholeFlagContainer = (LinearLayout) view.findViewById(R.id.whole_flag_container);
        imageHolder = (LinearLayout) view.findViewById(R.id.imageContainer);
        imageView = (ImageView) view.findViewById(R.id.pic);
        commentsView = (TableLayout) view.findViewById(R.id.comments);
        playVideoButton = (ImageView) view.findViewById(R.id.play_video_button);
        videoHolder = (FrameLayout) view.findViewById(R.id.video_holder);
        videoView = (VideoView) view.findViewById(R.id.vid);
        audioLayout = (LinearLayout) view.findViewById(R.id.audioContainer);
        audioHolder = (ImageView) view.findViewById(R.id.audio);
        wowStatText = (TextView) view.findViewById(R.id.wow_stats);
        newWowButton = (ToggleButton) view.findViewById(R.id.wow_button);
        newWowButton.setTransformationMethod(null); //to avoid capital letters in lollipop

        imageView.setOnClickListener(this);
        videoView.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
        newWowButton.setOnClickListener(this);
        setWowButton(); // to manage pressed effect when opening flag
        profilePicImageView.setOnClickListener(this);
        Button addCommentButton = (Button) view.findViewById(R.id.add_comment);
        addCommentButton.setOnClickListener(this);
        addCommentButton.setTransformationMethod(null);//to avoid capital letters in lollipop

        this.changeLayoutAccordingToMediaType();
        flagText.setText(this.flag.getText());

        //TODO still necessary?
            /*
            FacebookUtils.getInstance().getFacebookUsernameFromID(this.id, new FacebookUtilCallback() {
                @Override
                public void onResult(String result, Exception e) {
                    authorTextView.setText("Author: " + result + bottomLineText);
                }
            });
            */

        // authorTextView.setText(((PlacesUser) ParseUser.getCurrentUser()).getName());
        authorTextView.setText(this.flag.getFbName());
        final String inPlaceString = "In Place: " + (this.flag.getInPlace() ? "✓" : "✗");
        DateFormat df = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
        String sDate = df.format(this.flag.getDate());
        dateTextView.setText(sDate + '\n' + inPlaceString);

        PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(this.flag.getFbId(), profilePicImageView, PlacesLoginUtils.PicSize.LARGE, this.flag.getAccountType());

        view.setFocusableInTouchMode(true);
        view.requestFocus();

        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (frameLayout.getVisibility() == View.VISIBLE) {
                        frameLayout.setVisibility(View.GONE);
                        wholeFlagContainer.setVisibility(View.VISIBLE);
                        // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        return true;
                    }
                }
                return false;
            }
        });
        updateWowInfo();
        retrieveComments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "1) frame_layout visibility: " + ((frameLayout.getVisibility() == View.VISIBLE) ? "VISIBLE" : "NOT VISIBLE"));

        if (v.getId() == R.id.frame_layout) {
            Log.d(TAG, "frame_layout clicked!");
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            frameLayout.setVisibility(View.GONE);
            wholeFlagContainer.setVisibility(View.VISIBLE);
        }
        // TODO Enlarged media frame disabled
        /*
        else if (v.getId() == R.id.pic) {
            Log.d(TAG, "pic clicked!");
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            frameLayout.setVisibility(View.VISIBLE);
            //TODO image bigView

            wholeFlagContainer.setVisibility(View.GONE);
        }
        */
        // else if(v.getId() == playVideoButton.getId()) playVideo();
        else if (v.getId() == R.id.audio) playRecording();
        else if (v.getId() == R.id.wow_button) wlbFlag(WOW_CODE);
            // else if (v.getId() == R.id.lol_button) wlbFlag(LOL_CODE);
            // else if (v.getId() == R.id.boo_button) wlbFlag(BOO_CODE);
        else if (v.getId() == R.id.add_comment) insertComment();
        else if (v.getId() == R.id.profile_pic) showProfilePage();
        else Log.d(TAG, "don't really know what's been clicked!");

        Log.d(TAG, "2) frame_layout visibility: " + ((frameLayout.getVisibility() == View.VISIBLE) ? "VISIBLE" : "NOT VISIBLE"));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return v.getId() == videoView.getId() && event.getAction() == MotionEvent.ACTION_DOWN && playVideo();
    }

    private void showProfilePage() {
        ((MainActivity) getActivity()).switchToOtherFrag(ProfileFragment.newInstance(this.flag.getOwner()));
    }

    private void updateWowInfo() {
//        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
//        queryPosts.whereEqualTo("objectId", this.flag.getObjectId());
//        queryPosts.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
//        queryPosts.getFirstInBackground(new GetCallback<Flag>() {
//            @Override
//            public void done(Flag flag, ParseException e) {
//                if (e == null) {

        final int wowCount = flag.getInt("wowCount");

        ParseQuery<CustomParseObject> queryWoWs = ParseQuery.getQuery("Wow_Lol_Boo");
        queryWoWs.whereEqualTo("flag", flag);
        queryWoWs.whereEqualTo("user", ParseUser.getCurrentUser());
//        due to free parse api terms, better to use get over count
//        queryWoWs.countInBackground(new CountCallback() {
//            @Override
//            public void done(int i, ParseException e) {
//                if(e == null){
//                    Log.d(TAG, "WoW count: " + i);
//                    Log.d(TAG, "flagId: " + flag.getObjectId() + ", userId: " + ParseUser.getCurrentUser().getObjectId());
//                    if (wowCount == 1) wowStatText.setText("You WoWed this.");
//                    else if (wowCount == 2)
//                        wowStatText.setText("You and another Placer WoWed this.");
//                    else if (wowCount == 0) wowStatText.setText("");
//                    else wowStatText.setText("You and " + (wowCount > 2 ? (("other " + (wowCount - 1) + " Placers WoWed this."))
//                                : ("another Placer WoWed this.")));
//                }else{
//                    Log.e(TAG, e.getMessage());
//                    Log.e(TAG, "Error on user: " + ParseUser.getCurrentUser().getUsername());
//                    if (wowCount > 0) wowStatText.setText((wowCount +
//                            (wowCount == 1 ? " Placer WoWed this." : " Placers WoWed this.")));
//                    else wowStatText.setText("");
//                }
//            }
//        });
        queryWoWs.getFirstInBackground(new GetCallback<CustomParseObject>() {
            @Override
            public void done(CustomParseObject wow, ParseException e) {
                if (e == null) {
                    if (wowCount == 1) wowStatText.setText("You WoWed this.");
                    else if (wowCount == 2)
                        wowStatText.setText("You and another Placer WoWed this.");
                    else if (wowCount == 0) wowStatText.setText("");
                    else
                        wowStatText.setText("You and " + (wowCount > 2 ? (("other " + (wowCount - 1) + " Placers WoWed this."))
                                : ("another Placer WoWed this.")));
                } else {
                    Log.d(TAG, e.getMessage());
                    Log.d(TAG, "Current user: " + ParseUser.getCurrentUser().getObjectId() +
                            ", didn't WoW Flag: " + flag.getObjectId());
                    if (wowCount > 0) wowStatText.setText((wowCount +
                            (wowCount == 1 ? " Placer WoWed this." : " Placers WoWed this.")));
                    else wowStatText.setText("");
                }
            }
        });
//                } else {
//                    Log.e(TAG, e.getMessage());
////                    Always triggers the toast on start
////                    Utils.showToast(getActivity(), "An error occurred while retrieving Flag data", Toast.LENGTH_SHORT);
//                }
//            }
//        });
    }

    private void retrieveComments() {
        new CommentsManager().commentsForFlag(this.flag)
                .cache(ParseQuery.CachePolicy.CACHE_THEN_NETWORK)
                .sort(new Comparator<Comment>() {
                    @Override
                    public int compare(Comment lhs, Comment rhs) {
                        return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
                    }
                })
                .success(new ModelCallback<Comment>() {
                    @Override
                    public void result(List<Comment> result) {
                        if (result == null || result.size() == 0) result = new ArrayList<>();

                        commentsView.removeAllViews();

                        for (Comment c : result) {
                            commentsView.addView(getTableRowView(c));
                        }

                        if (scrollToLastComment && result.size() > 0) lastCommentPos.requestFocus();

                        scrollToLastComment = false;
                    }
                })
                .start();
    }

    private void insertComment() {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View passwordDialogLayout = li.inflate(R.layout.comment_insertion_layout, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(passwordDialogLayout);

        final EditText userInput = (EditText) passwordDialogLayout.findViewById(R.id.new_comment);

        alertDialogBuilder
                .setCancelable(true)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton("Confirm",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                newComment = userInput.getText().toString();
                                if (newComment.length() == 0) {
                                    Toast.makeText(getActivity(), "Comment cannot be empty!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                final Comment comment = new Comment();
                                comment.put("text", newComment);
                                comment.put("userId", PlacesLoginUtils.getInstance().getCurrentUserId());
                                comment.put("flagId", FlagFragment.this.flag.getObjectId());
                                comment.put("accountType", (PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.FACEBOOK) ? "fb" : "g+");
                                comment.put("flag", FlagFragment.this.flag);

                                String username = ((PlacesUser) ParseUser.getCurrentUser()).getName();
                                if (username == null)
                                    username = PlacesLoginUtils.getInstance().getUserNameFromId(FlagFragment.this.flag.getFbId());
                                if (username != null) comment.setUsername(username);
                                comment.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            PlacesUtils.showToast(getActivity(), "Something went wrong while commenting", Toast.LENGTH_SHORT);
                                            Log.e(TAG, e.getMessage());
                                        }
                                        scrollToLastComment = true;
                                        retrieveComments();
                                    }
                                });

                                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                                dialog.dismiss();
                            }
                        }
                );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        userInput.requestFocus();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private View getTableRowView(Comment c) {
        TableRow tr = new TableRow(getActivity());
        final View v = LayoutInflater.from(getActivity()).inflate(R.layout.comment_item_layout, tr, false);
        // TODO Change views according to comment

        String user_id = c.getUserId();
        String account_type = c.getAccountType();
        String fb_username = c.getUsername(); // checks if Flag has fb username. if there is one use it otherwise ask FB

        TextView usernameTextView = (TextView) v.findViewById(R.id.author);
        final ImageView profilePic = (ImageView) v.findViewById(R.id.comment_profile_pic);
        TextView commentText = (TextView) v.findViewById(R.id.comment_text);
        TextView commentDate = (TextView) v.findViewById(R.id.comment_date);

        // If user is logged in with G+, FB Graph API cannot be used
        if (fb_username == null)
            PlacesLoginUtils.getInstance().loadUsernameIntoTextView(user_id, usernameTextView, account_type);
        else {
            if (!PlacesLoginUtils.getInstance().getUserIdMap().containsKey(user_id))
                PlacesLoginUtils.getInstance().addEntryToUserIdMap(user_id, fb_username);
            usernameTextView.setText(fb_username);
        }


        PlacesLoginUtils.getInstance().getProfilePictureURL(user_id, account_type, PlacesLoginUtils.PicSize.SMALL, new PlacesUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if (result != null && !result.isEmpty()) {
                    Picasso.with(v.getContext()).load(result).into(profilePic);
                }
            }
        });

        Date date = c.getTimestamp();
        DateFormat df = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
        String sDate = df.format(date);
        commentDate.setText(sDate);

        commentText.setText(c.getCommentText());

        return v;//have to return View child, so return made 'v'
    }

    private void playRecording() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            audioHolder.setImageResource(R.drawable.play_button);
        } else {
            mediaPlayer.start();
            audioHolder.setImageResource(R.drawable.pause_button);
        }
    }

    private boolean playVideo() {
        if (videoView.isPlaying()) {
            videoView.pause();
            playVideoButton.setVisibility(View.VISIBLE);
        } else {
            videoView.start();
            playVideoButton.setVisibility(View.GONE);
        }
        return true;
    }

    private void onVideoDownloaded(String videoPath) {
        Uri videoUri = Uri.parse(videoPath);
        videoView.setVideoURI(videoUri);

        try {
            videoView.seekTo(10);
        } catch (RuntimeException re) {
            Log.d(TAG, "Video is too short to show preview");
        }

        playVideoButton.setVisibility(View.VISIBLE);
        // playVideo();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playVideoButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onAudioDownloaded(String audioPath) {
        try {
            audioLayout.setVisibility(View.VISIBLE);
            File temp = new File(audioPath);
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    audioHolder.setImageResource(R.drawable.play_button);
                }
            });
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            FileInputStream inStream = new FileInputStream(temp);
            mediaPlayer.setDataSource(inStream.getFD());
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void wlbFlag(int code) {
        newWowButton.setClickable(false);

        ParseQuery<CustomParseObject> queryWLB = ParseQuery.getQuery("Wow_Lol_Boo");
        queryWLB.whereEqualTo("user", PlacesUser.getCurrentUser());
        queryWLB.whereEqualTo("flagId", this.flag.getObjectId());

        switch (code) {
            case WOW_CODE:
                queryWLB.getFirstInBackground(new GetCallback<CustomParseObject>() {
                    @Override
                    public void done(CustomParseObject obj, ParseException e) {
                        if (e == null) {
                            boolean boolWow = obj.getBoolean("boolWow");

                            if (!boolWow) {
                                obj.setWowBoolean(true);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(WOW_CODE, true);
                                    }
                                });
                            } else {
                                obj.setWowBoolean(false);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(WOW_CODE, false);
                                    }
                                });
                            }
                        } else if (obj == null) {
                            obj = new CustomParseObject();
                            obj.setUser(PlacesUser.getCurrentUser());
                            obj.setFlagId(FlagFragment.this.flag.getObjectId());
                            obj.setFlag(FlagFragment.this.flag);
                            obj.setFacebookId(PlacesLoginUtils.getInstance().getCurrentUserId());
                            obj.setWowBoolean(true);
                            obj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    updateWLBCount(WOW_CODE, true);
                                }
                            });

                            Log.d(TAG, "No suitable entry found in wow table: " + e.getMessage());
                        } else {
                            Toast.makeText(getActivity(), "Error encountered while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encountered while retrieving table entry on Parse.com");
                        }
                    }
                });
                break;

            default:
                break;
        }
    }

    private void updateWLBCount(int code, final boolean increment) {
        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", this.flag.getObjectId());

        switch (code) {
            case WOW_CODE:
                queryPosts.getFirstInBackground(new GetCallback<Flag>() {
                    @Override
                    public void done(Flag flag, ParseException e) {
                        if (e == null) {
                            final int wowCount = flag.getInt("wowCount");

                            if (increment) {
                                flag.increment("wowCount");
                                flag.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            updateWowStats(true, wowCount + 1);
                                        } else {
                                            Log.e(TAG, "Error", e);
                                            PlacesUtils.showToast(getActivity(), "Something went wrong while updating WoW count", Toast.LENGTH_SHORT);
                                        }
                                        newWowButton.setClickable(true);
                                    }
                                });
                            } else {
                                flag.increment("wowCount", -1);
                                flag.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            updateWowStats(false, wowCount - 1);
                                        } else {
                                            Log.e(TAG, "Error", e);
                                            PlacesUtils.showToast(getActivity(), "Something went wrong while updating WoW count", Toast.LENGTH_SHORT);
                                        }
                                        newWowButton.setClickable(true);
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(getActivity(), "Error encountered while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encountered while retrieving table entry on Parse.com");

                            newWowButton.setClickable(true);
                        }
                    }
                });
                break;

            default:
                break;
        }
    }

    private void updateWowStats(boolean wowed, int wowCount) {
        Log.d(TAG, "wowed: " + wowed + ", wowCount: " + wowCount);

        wowStatText.setVisibility(View.VISIBLE);

        if (wowed) {
            if (wowCount == 1) {
                wowStatText.setText("You WoWed this.");
                newWowButton.setChecked(true);
            } else {
                wowStatText.setText("You and " + (wowCount > 2 ? (("other " + (wowCount - 1) + " Placers WoWed this."))
                        : ("another Placer WoWed this.")));
                newWowButton.setChecked(true);
            }
        } else {
            newWowButton.setChecked(false);
            if (wowCount == 0) {
                Log.d(TAG, "wowCount is 0");
                wowStatText.setText("");
                wowStatText.setVisibility(View.INVISIBLE);
            } else {
                wowStatText.setText((wowCount +
                        (wowCount == 1 ? " Placer WoWed this." : " Placers WoWed this.")));
            }
        }
    }

    private void changeLayoutAccordingToMediaType() {
        if (mediaType == MediaType.NONE) {
        } else if (mediaType == MediaType.AUDIO) {
            audioLayout.setVisibility(View.VISIBLE);
        } else if (mediaType == MediaType.PIC) {
            imageHolder.setVisibility(View.VISIBLE);

            ParseFile thumbnail = this.flag.getThumbnail();
            final ParseFile picture = this.flag.getPic();
            if (thumbnail != null) {
                Picasso.with(this.getActivity()).load(thumbnail.getUrl()).into(this.imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.with(FlagFragment.this.getActivity()).load(picture.getUrl()).into(FlagFragment.this.imageView);
                    }

                    @Override
                    public void onError() {
                    }
                });
            } else {
                Picasso.with(this.getActivity()).load(picture.getUrl()).into(this.imageView);
            }
            if (this.getView() != null) {
                ImageView focused_imageView = (ImageView) this.getView().findViewById(R.id.pic_big_preview);
                Picasso.with(this.getActivity()).load(picture.getUrl()).into(focused_imageView);
            }


        } else {
            videoHolder.setVisibility(View.VISIBLE);
        }
        if (this.mediaType != MediaType.NONE && this.mediaType != MediaType.PIC && this.mediaFile != null) {
            System.gc();

            this.mediaFile.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if (!FlagFragment.this.isAdded()) {
                        //horrible hack to prevent a crash when the Fragment is detached from an activity.
                        return;
                    }
                    if (e == null) {
                        try {
                            MediaType mediaType = FlagFragment.this.mediaType;
                            File tempFile = FlagFragment.this.tempFileForMediaType(mediaType);
                            FileOutputStream outputStream = new FileOutputStream(tempFile);
                            outputStream.write(bytes);
                            outputStream.flush();
                            outputStream.close();

                            if (mediaType == MediaType.AUDIO) {
                                FlagFragment.this.onAudioDownloaded(tempFile.getAbsolutePath());
                            } else if (mediaType == MediaType.VIDEO) {
                                FlagFragment.this.onVideoDownloaded(tempFile.getAbsolutePath());
                            }
                            FlagFragment.this.mediaFile = null;

                        } catch (IOException io) {
                            Log.d(TAG, "IO Error", io);
                            Toast.makeText(FlagFragment.this.getActivity(), "Error downloading file", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "Download Error", e);
                        Toast.makeText(FlagFragment.this.getActivity(), "Error downloading file", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private File tempFileForMediaType(MediaType type) {
        String fileName;
        String fileFormat;
        switch (type) {
            case AUDIO:
                fileFormat = ShareActivity.AUDIO_FORMAT;
                fileName = "places_temp_audio";
                break;
            case VIDEO:
                fileFormat = ShareActivity.VIDEO_FORMAT;
                fileName = "places_temp_video";
                break;
            case PIC:
                fileFormat = ShareActivity.PICTURE_FORMAT;
                fileName = "places_temp_pic";
                break;
            default:
                return null;
        }
        try {
            Log.d(TAG, "name " + fileName);
            Log.d(TAG, "format " + fileFormat);
            return File.createTempFile(fileName, fileFormat, this.getActivity().getCacheDir());
        } catch (IOException e) {
            Log.d(TAG, "Cannot create temp file", e);
            return null;
        }
    }

    public static enum MediaType {PIC, AUDIO, VIDEO, NONE}

    //similar to wlbFlag but just to initialize the toggleButton of WOW in the correct way
    private void setWowButton() {
        ParseQuery<CustomParseObject> queryWLB = ParseQuery.getQuery("Wow_Lol_Boo");
        queryWLB.whereEqualTo("user", ParseUser.getCurrentUser());
        queryWLB.whereEqualTo("flag", this.flag);
        queryWLB.findInBackground(new FindCallback<CustomParseObject>() {
            @Override
            public void done(List<CustomParseObject> markers, ParseException e) {
                if (e == null /*  && markers.size() != 0   */) {
                    //with old flags, when markers size is 0
                    if (markers.size() == 0)
                        newWowButton.setChecked(false);
                    else {
                        CustomParseObject obj = markers.get(0);

                        boolean boolWow = obj.getBoolean("boolWow");
                        if (!boolWow) newWowButton.setChecked(false);
                        else newWowButton.setChecked(true);
                    }
                } else {
                    Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");
                    newWowButton.setChecked(false);
                    //wowStatText.setClickable(true);
                }
            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /*
        CommentsAdapter fa = (CommentsAdapter) commentsRecyclerView.getAdapter();
        Comment sel_usr = fa.getSelectedComment();

        if (sel_usr == null)
            Toast.makeText(getActivity(), Utils.NO_VALID_COMMENT_SELECTED, Toast.LENGTH_SHORT).show();

        switch (item.getItemId()) {

            case Utils.DELETE_COMMENT:
                deleteComment(sel_usr);
                fa.setSelectedCommentIndex(-1);
                return true;

            case Utils.REPORT_COMMENT:
                this.reportComment(sel_usr);
                fa.setSelectedCommentIndex(-1);
                return true;

            case Utils.DELETE_REPORT_COMMENT:
                this.deleteReportComment(sel_usr);
                fa.setSelectedCommentIndex(-1);
                return true;

            default:
                fa.setSelectedCommentIndex(-1);
                return super.onContextItemSelected(item);
        }
        */

        return false;
    }

    private void deleteComment(Comment comment) {
        comment.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Toast.makeText(getActivity(), PlacesUtils.COMMENT_DELETED, Toast.LENGTH_SHORT).show();
                    retrieveComments();
                } else
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reportComment(final Comment comment) {
        CommentReport report = CommentReport.createFlagReportFromFlag(comment);
        report.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Toast.makeText(getActivity(), PlacesUtils.COMMENT_REPORTED, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Deletes the entry related to @param comment, from the Reported_Comments table
    private void deleteReportComment(Comment comment) {
        ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Comments");

        queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
        queryDelete.whereEqualTo("reported_flag", comment);

        queryDelete.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject p, ParseException e) {
                if (e == null) {
                    p.deleteInBackground(new DeleteCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getActivity(), PlacesUtils.FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}