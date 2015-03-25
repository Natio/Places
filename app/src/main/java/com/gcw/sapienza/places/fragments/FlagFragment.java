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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.activities.ShareActivity;
import com.gcw.sapienza.places.adapters.CommentsAdapter;
import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.CommentReport;
import com.gcw.sapienza.places.models.CustomParseObject;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.Utils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by mic_head on 02/02/15.
 */

public class FlagFragment extends Fragment implements View.OnClickListener, View.OnTouchListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "FlagFragment";
    private static final int WOW_CODE = 0;
    private static final int LOL_CODE = 1;
    private static final int BOO_CODE = 2;

    protected String userId;
    private String text;
    private String id;
    private String date;
    private String weather;
    private String temperature;
    private String weatherInfo;
    private String category;
    private String flagId;
    private String author;
    private String accountType;
    private String newComment;

    private boolean inPlace;
    private boolean scrollToLastComment;

    private int wowCount;
    private MediaPlayer mediaPlayer;
    private VideoView vv;
    private ImageView iw;
    private ImageView weatherIco;
    private ImageView categoryIco;
    private ImageView playVideoButton;
    private ImageView profilePicImageView;
    private ImageView audioHolder;
    private SwipeRefreshLayout commentsHolder;
    private TextView authorTextView;
    private TextView dateTextView;
    private TextView flagText;
    private TextView temperatureView;
    private TextView wowStatText;
    private LinearLayout wholeFlagContainer;
    private RelativeLayout flagContainer;
    private RelativeLayout frameLayout;
    private LinearLayout audioLayout;
    private LinearLayout imageContainer;
    private LinearLayout imageHolder;
    private FrameLayout videoHolder;
    private RecyclerView rv;
    private Button addCommentButton;
    private ToggleButton newWowButton;
    private CommentsAdapter commentsAdapter;
    private ArrayList<String> comments;

    private MediaType mediaType;
    private ParseFile mediaFile;
    private View view;
    private StringTokenizer st;
    private ScrollView flagContent;
    private Flag flag;
    private ArrayList<Comment> commentObjs;

    /**
     * Must be called BEFORE adding the fragment to the
     *
     * @param mediaFile the file to display
     * @param type      the type of the file
     */
    public void setMedia(ParseFile mediaFile, MediaType type) {
        this.mediaType = type;
        this.mediaFile = mediaFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        scrollToLastComment = bundle.getBoolean("scrollToLastComment");

        text = bundle.getString("text");
        id = bundle.getString("id");
        date = bundle.getString("date");

        weatherInfo = bundle.getString("weather");

        //temporal bypass of a weather bug, "IF" to be deleted and keep only the else content
        if (weatherInfo.equals("")) {
            temperature = "" + 20;
            weather = "Cloud";
        } else {
            st = new StringTokenizer(weatherInfo, ",");
            temperature = st.nextToken();
            weather = st.nextToken().substring(1);
        }

        //Log.d(TAG, "weather-" + weather + '-' + temperature);

        category = bundle.getString("category");
        inPlace = bundle.getBoolean("inPlace");
        flagId = bundle.getString("flagId");
        author = bundle.getString("author");
        wowCount = bundle.getInt("wowCount");
        accountType = bundle.getString("accountType");

        userId = id;
    }

    public void setFlag(Flag f){
        this.flag = f;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.flag_layout, container, false);
        flagContent = (ScrollView)view.findViewById(R.id.FlagContent);

        rv = (RecyclerView)view.findViewById(R.id.comments_holder);
        LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);

        flagText = (TextView) view.findViewById(R.id.text);
        authorTextView = (TextView) view.findViewById(R.id.author);
        dateTextView = (TextView) view.findViewById(R.id.dateInfo);

        categoryIco = (ImageView) view.findViewById(R.id.categoryIcon);
        weatherIco = (ImageView) view.findViewById(R.id.meteo);
        temperatureView = (TextView) view.findViewById(R.id.temperature);
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

        flagContainer = (RelativeLayout) view.findViewById(R.id.flag_container);

        profilePicImageView = (ImageView) view.findViewById(R.id.profile_pic);

        frameLayout = (RelativeLayout) view.findViewById(R.id.frame_layout);
        wholeFlagContainer = (LinearLayout) view.findViewById(R.id.whole_flag_container);

        //names need to be changed in a coherent way, holder,container, iw vv ... etcetc
        imageHolder = (LinearLayout) view.findViewById(R.id.imageContainer);
        iw = (ImageView) view.findViewById(R.id.pic);

        commentsHolder = (SwipeRefreshLayout)view.findViewById(R.id.comments);

        playVideoButton = (ImageView) view.findViewById(R.id.play_video_button);
        videoHolder = (FrameLayout) view.findViewById(R.id.video_holder);
        vv = (VideoView) view.findViewById(R.id.vid);

        audioLayout = (LinearLayout) view.findViewById(R.id.audioContainer);
        audioHolder = (ImageView) view.findViewById(R.id.audio);

        wowStatText = (TextView) view.findViewById(R.id.wow_stats);
        newWowButton = (ToggleButton) view.findViewById(R.id.wow_button);
        //to avoid capital letters in lollipop
        newWowButton.setTransformationMethod(null);

        iw.setOnClickListener(this);
        vv.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        frameLayout.setOnClickListener(this);

        newWowButton.setOnClickListener(this);
        setWowButton(); // to manage pressed effect when opening flag
        profilePicImageView.setOnClickListener(this);

        commentsHolder.setOnRefreshListener(this);
        addCommentButton = (Button) view.findViewById(R.id.add_comment);
        addCommentButton.setOnClickListener(this);
        //to avoid capital letters in lollipop
        addCommentButton.setTransformationMethod(null);

        this.changeLayoutAccordingToMediaType();
        flagText.setText(text);

            /*
            FacebookUtils.getInstance().getFacebookUsernameFromID(this.id, new FacebookUtilCallback() {
                @Override
                public void onResult(String result, Exception e) {
                    authorTextView.setText("Author: " + result + bottomLineText);
                }
            });
            */
        //Strings to show
        //authorTextView.setText("Author: " + ((PlacesUser) ParseUser.getCurrentUser()).getName() + bottomLineText);
        //final String weatherString = (weather == null || weather.isEmpty()) ? "" : "\nWeather: " + weather;
        //final String inPlaceString = "In Place: " + (inPlace ? "✓" : "✗");
        //final String bottomLineText = date + weatherString + "\nCategory: " + category + "\n" + inPlaceString;

        // authorTextView.setText(((PlacesUser) ParseUser.getCurrentUser()).getName());
        authorTextView.setText(author);

        //changed to icons
        //final String weatherString = (weather == null || weather.isEmpty()) ? "" : "\nWeather: " + weather;
        //final String bottomLineText = "Category: " + category;
        final String inPlaceString = "In Place: " + (inPlace ? "✓" : "✗");

        dateTextView.setText(date + '\n' + inPlaceString);

        PlacesLoginUtils.getInstance().loadProfilePicIntoImageView(this.id, profilePicImageView, PlacesLoginUtils.PicSize.LARGE, accountType);

        view.setFocusableInTouchMode(true);
        view.requestFocus();

        view.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    if (frameLayout.getVisibility() == View.VISIBLE)
                    {
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
    public void onRefresh() {
        commentsHolder.setRefreshing(true);
        retrieveComments();
        commentsHolder.setRefreshing(false);
    }

    @Override
    public void onClick(View v)
    {
        Log.d(TAG, "1) frame_layout visibility: " + ((frameLayout.getVisibility() == View.VISIBLE) ? "VISIBLE" : "NOT VISIBLE"));

        if (v.getId() == R.id.frame_layout) {
            Log.d(TAG, "frame_layout clicked!");
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            frameLayout.setVisibility(View.GONE);
            wholeFlagContainer.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.pic) {
            Log.d(TAG, "pic clicked!");
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            frameLayout.setVisibility(View.VISIBLE);
            wholeFlagContainer.setVisibility(View.GONE);
        }
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
        if (v.getId() == vv.getId() && event.getAction() == MotionEvent.ACTION_DOWN) {
            return playVideo();
        }
        return false;
    }

    private void showProfilePage() {
        ((MainActivity) getActivity()).switchToOtherFrag(ProfileFragment.newInstance(userId, accountType));
    }

    private void updateWowInfo() {
        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", flagId);

        queryPosts.getFirstInBackground(new GetCallback<Flag>() {
            @Override
            public void done(Flag flag, ParseException e) {
                if (e == null)
                {
                    wowCount = flag.getInt("wowCount");
                }
            }
        });

        ParseQuery<CustomParseObject> queryW = ParseQuery.getQuery("Wow_Lol_Boo");
        queryW.whereEqualTo("fbId", userId);
        queryW.whereEqualTo("flagId", flagId);
        queryW.whereEqualTo("boolWow", true);

        queryW.getFirstInBackground(new GetCallback<CustomParseObject>()
        {
            @Override
            public void done(CustomParseObject obj, ParseException e)
            {
                if (e == null && obj != null)
                {
                    if (wowCount == 1) wowStatText.setText("You WoWed this.");
                    else if (wowCount == 2) wowStatText.setText("You and another placer WoWed this.");
                    else wowStatText.setText("You and other " + wowCount + " WoWed this.");
                }
            }
        });
    }

    private void retrieveComments()
    {
        ParseQuery<Comment> query = ParseQuery.getQuery("Comments");
        query.whereEqualTo("flagId", flagId);
        query.orderByAscending("createdAt");

        query.findInBackground(new FindCallback<Comment>()
        {
            @Override
            public void done(List<Comment> result, ParseException e)
            {
                if (result == null || result.size() == 0)
                {
                    ArrayList<Comment> emptyCommentList = new ArrayList<>();
                    commentsAdapter = new CommentsAdapter(emptyCommentList, rv, getActivity());
                }
                else
                {
                    comments = new ArrayList<>();
                    commentObjs = new ArrayList<>();

                    for (Comment comment : result)
                    {
                        comments.add(comment.getObjectId());
                        commentObjs.add(comment);
                    }

                    commentsAdapter = new CommentsAdapter(commentObjs, rv, getActivity());
                }

                rv.setAdapter(commentsAdapter);
                if(scrollToLastComment)
                {
                    rv.scrollToPosition(commentObjs.size() - 1);
                    scrollToLastComment = false;
                }
            }
        });
    }

    private void insertComment() {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View passwordDialogLayout = li.inflate(R.layout.comment_insertion_layout, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(passwordDialogLayout);

        final EditText userInput = (EditText) passwordDialogLayout.findViewById(R.id.new_comment);

        // final InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        alertDialogBuilder
                .setCancelable(true)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
                                comment.put("flagId", flagId);
                                comment.put("accountType", (PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.FACEBOOK) ? "fb" : "g+");
                                comment.put("flag", ParseObject.createWithoutData("Posts", flag.getObjectId()));

                                    /*
                                    FacebookUtils.getInstance().getFacebookUsernameFromID(PlacesLoginUtils.getInstance().getCurrentUserId(), new FacebookUtilCallback() {
                                        @Override
                                        public void onResult(String result, Exception e)
                                        {
                                            comment.setUsername(result);
                                            comment.saveInBackground();
                                        }
                                    });
                                    */

                                String username = ((PlacesUser) ParseUser.getCurrentUser()).getName();
                                if(username == null) username = PlacesLoginUtils.getInstance().getUserNameFromId(userId);
                                if(username != null) comment.setUsername(username);
                                comment.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            Utils.showToast(getActivity(), "Something went wrong while commenting", Toast.LENGTH_SHORT);
                                            Log.e(TAG, e.getMessage());
                                        }
                                        retrieveComments();
                                    }
                                });

                                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0);

                                dialog.dismiss();
                            }

                        }

                );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        userInput.requestFocus();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
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
        if (vv.isPlaying()) {
            vv.pause();
            playVideoButton.setVisibility(View.VISIBLE);
        } else {
            // vv.resume();
            vv.start();
            playVideoButton.setVisibility(View.GONE);
        }

        return true;
    }

    private void onVideoDownloaded(String videoPath) {
        Uri videoUri = Uri.parse(videoPath);
        vv.setVideoURI(videoUri);

        try {
            vv.seekTo(10);
        } catch (RuntimeException re) {
            Log.d(TAG, "Video is too short to show preview");
        }

        playVideoButton.setVisibility(View.VISIBLE);
        // playVideo();

        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playVideoButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onAudioDownloaded(String audioPath) {
        try {
            //audioHolder.setVisibility(View.VISIBLE);
            audioLayout.setVisibility(View.VISIBLE);

            //videoHolder.setVisibility(View.GONE);


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
        ParseQuery<CustomParseObject> queryWLB = ParseQuery.getQuery("Wow_Lol_Boo");
        queryWLB.whereEqualTo("user", PlacesUser.getCurrentUser());
        queryWLB.whereEqualTo("flagId", flagId);

        switch (code) {
            case WOW_CODE:
                queryWLB.getFirstInBackground(new GetCallback<CustomParseObject>() {
                    @Override
                    public void done(CustomParseObject obj, ParseException e)
                    {
                        if (e == null)
                        {
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
                        }
                        else if (obj == null)
                        {
                            obj = new CustomParseObject();
                            obj.setUser(PlacesUser.getCurrentUser());
                            obj.setFlagId(flagId);
                            obj.setFacebookId(PlacesLoginUtils.getInstance().getCurrentUserId());
                            obj.setWowBoolean(true);
                            obj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    updateWLBCount(WOW_CODE, true);
                                }
                            });

                            Log.d(TAG, "No suitable entry found in wow table: " + e.getMessage());
                        }
                        else
                        {
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

    private void updateWLBCount(int code, final boolean increment)
    {
        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", flagId);

        switch (code) {
            case WOW_CODE:
                queryPosts.getFirstInBackground(new GetCallback<Flag>() {
                    @Override
                    public void done(Flag flag, ParseException e)
                    {
                        if (e == null)
                        {
                            final int wowCount = flag.getInt("wowCount");

                            if (increment) {
                                flag.increment("wowCount");
                                flag.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWowStats(true, wowCount + 1);
                                    }
                                });
                            } else {
                                flag.increment("wowCount", -1);
                                flag.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWowStats(false, wowCount - 1);
                                    }
                                });
                            }
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

    private void updateWowStats(boolean wowed, int wowCount)
    {
        Log.d(TAG, "wowed: " + wowed + ", wowCount: " + wowCount);

        wowStatText.setVisibility(View.VISIBLE);

        if (wowed) {
            if (wowCount == 1) {
                wowStatText.setText("You WoWed this.");
                newWowButton.setChecked(true);
            } else {
                wowStatText.setText("You and other " + (wowCount - 1) + " WoWed.");
                newWowButton.setChecked(true);
            }
        } else {
            newWowButton.setChecked(false);
            if (wowCount==0)
            {
                Log.d(TAG, "wowCount is 0");
                wowStatText.setText("");
                wowStatText.setVisibility(View.INVISIBLE);
            }
            else{
                if(wowCount==1){
                    wowStatText.setText("1 WoW");
                }
                else
                    wowStatText.setText(wowCount + " WoWs");
            }
        }
    }
/*
    private void updateLolButtonText(boolean lold, int lolCount) {
        if (lold) lolButton.setText("You lol this. (" + lolCount + ')');
        else lolButton.setText("LOL (" + lolCount + ')');
    }

    private void updateBooButtonText(boolean booed, int booCount) {
        if (booed) booButton.setText("You boo this. (" + booCount + ')');
        else booButton.setText("BOO (" + booCount + ')');
    }
*/
    private void changeLayoutAccordingToMediaType() {
        if (mediaType == MediaType.NONE) {
            // TODO managing resizing of flags with very short text
            //maybe i'll do a swipe up and down...as googleMap effect
        } else if (mediaType == MediaType.AUDIO) {
            audioLayout.setVisibility(View.VISIBLE);
        } else if (mediaType == MediaType.PIC) {
            imageHolder.setVisibility(View.VISIBLE);


            ParseFile thumbnail = this.flag.getThumbnail();
            final ParseFile picture = this.flag.getPic();
            if(thumbnail != null){
                Picasso.with(this.getActivity()).load(thumbnail.getUrl()).into(this.iw, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.with(FlagFragment.this.getActivity()).load(picture.getUrl()).into(FlagFragment.this.iw);
                    }

                    @Override
                    public void onError() {

                    }
                });
            }
            else{
                Picasso.with(this.getActivity()).load(picture.getUrl()).into(this.iw);
            }
            ImageView focused_imageView = (ImageView) this.view.findViewById(R.id.focused_pic);
            Picasso.with(this.getActivity()).load(picture.getUrl()).into(focused_imageView);

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
        queryWLB.whereEqualTo("fbId", userId);
        queryWLB.whereEqualTo("flagId", flagId);
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
                        if (!boolWow) {
                            newWowButton.setChecked(false);
                                        /*
                                        obj.setWowBoolean(true);
                                        obj.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                updateWLBCount(WOW_CODE, true);
                                            }
                                        });*/
                        } else {
                            newWowButton.setChecked(true);
                                        /*
                                        obj.setWowBoolean(false);
                                        obj.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                updateWLBCount(WOW_CODE, false);
                                            }
                                        });*/
                        }
                    }
                }
                                /*
                        else if (markers.size() == 0) {
                                    CustomParseObject obj = new CustomParseObject();
                                    obj.setUser(ParseUser.getCurrentUser());
                                    obj.setFlagId(flagId);
                                    obj.setFacebookId(PlacesLoginUtils.getInstance().getCurrentUserId());
                                    obj.setWowBoolean(true);
                                    obj.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            updateWLBCount(WOW_CODE, true);
                                        }
                                    });
                                } */
                else {
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
        CommentsAdapter fa = (CommentsAdapter) rv.getAdapter();
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
    }

    /**
     * Deletes the comment
     *
     * @param f comment to delete
     */
    private void deleteComment(Comment f) {
        f.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Toast.makeText(rv.getContext(), Utils.COMMENT_DELETED, Toast.LENGTH_SHORT).show();
                    retrieveComments();
                } else
                    Toast.makeText(rv.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Reports a comment
     *
     * @param f comment to report
     */
    private void reportComment(final Comment f) {

        CommentReport report = CommentReport.createFlagReportFromFlag(f);
        report.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Toast.makeText(rv.getContext(), Utils.COMMENT_REPORTED, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(rv.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Deletes an entry from the Reported_Comments table
     *
     * @param f comment related to the entry to be deleted
     */
    private void deleteReportComment(Comment f) {
        ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Comments");

        queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
        queryDelete.whereEqualTo("reported_flag", f);

        queryDelete.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject p, ParseException e) {
                if (e == null) {
                    p.deleteInBackground(new DeleteCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(rv.getContext(), Utils.FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(rv.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(rv.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}