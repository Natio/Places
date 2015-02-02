package com.gcw.sapienza.places;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.activities.MainActivity2;
import com.gcw.sapienza.places.activities.VideoCaptureActivity;
import com.gcw.sapienza.places.adapters.MSpinnerAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.FlagUploader;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseAnalytics;
import com.parse.ParseGeoPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mic_head on 02/02/15.
 */
public class ShareFragment2 extends Fragment implements View.OnLongClickListener {

    private static final String TAG = "ShareFragment2";
    public static final String PICTURE_FORMAT = ".jpg";
    public static final String AUDIO_FORMAT = ".3gp";
    public static final String VIDEO_FORMAT = ".mp4";

    private static final int PIC_CODE = 0;
    private static final int AUDIO_CODE = 1;
    private static final int VIDEO_CODE = 2;

    private Spinner spinner;
    private TextView textView;
    private Button shareButton;
    private RelativeLayout progressBarHolder;
    private TextView progressTextView;

    private ImageButton picButton;
    private ImageButton micButton;
    private ImageButton vidButton;

    private Context mContext;

    private boolean isPicTaken = false;
    private boolean isVideoShoot = false;
    private boolean isSoundCaptured = false;

    private File pic;
    private File video;
    private File audio;

    protected static MediaRecorder audioRec;
    protected static String audio_filename;

    private File imageFile;

    private View view;

    private static final int ANIMATION_DURATION = 300;

    private static final String FLAG_PLACED_TEXT = "Flag has been placed!";
    private static final String FB_ID_NOT_FOUND_TEXT = "Couldn't retrieve your Facebook credentials\nPlease check your internet connection.";
    private static final String EMPTY_FLAG_TEXT = "Please insert text or take a picture";
    private static final String ENABLE_NETWORK_SERVICE_TEXT = "Please enable GPS/Network service";
    private static final String PIC_NOT_FOUND_TEXT = "Error encountered while retrieving picture\nFlag won't be stored";
    private static final String AUDIO_NOT_FOUND_TEXT = "Error encountered while retrieving recording\nFlag won't be stored";
    private static final String VIDEO_NOT_FOUND_TEXT = "Error encountered while retrieving video\nFlag won't be stored";
    private static final String ERROR_WHILE_RECORDING_TEXT = "Error encountered while recording";

    public void setVideo(String video){
        this.video = null;
        if(video != null){
            File f = new File(video);
            this.video = f.canRead() ? f : null;
        }

        this.isVideoShoot = this.video != null;

        if(this.vidButton != null)
        {
            int res =R.drawable.videocam_selector;
            if(this.isVideoShoot)
            {
                res = R.drawable.videocam_green_taken;
            }
            this.vidButton.setImageDrawable( getResources().getDrawable(res));
        }

    }

    public void setAudio(String audio)
    {
        this.audio = null;

        if(audio != null)
        {
            File f = new File(audio);
            this.audio = f.canRead() ? f : null;
        }

        this.isSoundCaptured = this.audio != null;

        if(this.micButton != null)
        {
            int res =R.drawable.mic_selector;
            if(this.isSoundCaptured)
            {
                res = R.drawable.mic_green_taken;
            }
            this.micButton.setImageDrawable( getResources().getDrawable(res));
        }

    }

    public void setPicture(String pic)
    {
        this.pic = null;
        if(pic != null)
        {
            File f = new File(pic);
            this.pic = f.canRead() ? f : null;
        }
        this.isPicTaken = this.pic != null;

        if(this.picButton != null)
        {
            int res =R.drawable.cam_selector;
            if(this.isPicTaken)
            {
                res = R.drawable.camera_green_taken;
            }

            this.picButton.setImageDrawable( getResources().getDrawable(res));
        }

    }

    public String getPicPath() {
        return this.pic == null ? null : this.pic.getAbsolutePath();
    }

    public String getVideoPath() {
        return this.video == null ? null : this.video.getAbsolutePath();
    }

    public String getAudioPath() {
        return this.video == null ? null : this.video.getAbsolutePath();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.activity_share, container, false);

        this.progressBarHolder = (RelativeLayout)view.findViewById(R.id.frame_layout);
        this.progressTextView = (TextView)view.findViewById(R.id.share_progress_text_view);

        this.picButton = (ImageButton)view.findViewById(R.id.pic_button);
        this.micButton = (ImageButton)view.findViewById(R.id.mic_button);
        this.vidButton = (ImageButton)view.findViewById(R.id.vid_button);

        //these lines are necessary for a correct visualization
        this.setPicture(this.getPicPath());
        this.setVideo(this.getVideoPath());
        this.setAudio(this.getAudioPath());

        this.picButton.setOnLongClickListener(this);
        this.micButton.setOnLongClickListener(this);
        this.vidButton.setOnLongClickListener(this);

        micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(isSoundCaptured)
                {
                    return false;
                }

                Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(Utils.VIBRATION_DURATION);

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {

                    restoreAlpha(AUDIO_CODE);

                    try
                    {
                        audio_filename = Utils.createAudioFile(ShareFragment.AUDIO_FORMAT, mContext).getAbsolutePath(); //Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp";
                        audioRec = new MediaRecorder();
                        audioRec.setAudioSource(MediaRecorder.AudioSource.MIC);
                        audioRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        audioRec.setOutputFile(audio_filename);
                        audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        audioRec.prepare();
                    } catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        Toast.makeText(mContext, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
                        Log.e(TAG, ERROR_WHILE_RECORDING_TEXT);
                    }
                    audioRec.start();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    if(audioRec == null)
                    {
                        Toast.makeText(mContext, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
                        Log.v(TAG, ERROR_WHILE_RECORDING_TEXT);
                        return true;
                    }
                    else try
                    {
                        audioRec.stop();
                        audioRec.release();
                        audioRec = null;
                    }
                    catch(RuntimeException re)
                    {
                        re.printStackTrace();
                        Toast.makeText(mContext, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.mic_green_taken));
                    File audio_file = new File(audio_filename);
                    try
                    {
                        FileInputStream inStream = new FileInputStream(audio_file);
                        setAudio(audio_filename);

                        changeAlphaBasedOnSelection(AUDIO_CODE);

                        inStream.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                return true;
            }
        });

        this.textView = (TextView)view.findViewById(R.id.share_text_field);
        this.textView.setGravity(Gravity.CENTER);

        this.shareButton = (Button)view.findViewById(R.id.share_button);
        this.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);
                share();

            }
        });

        this.spinner = (Spinner)view.findViewById(R.id.spinner);

        // ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.categories, R.layout.custom_spinner);
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        MSpinnerAdapter adapter = new MSpinnerAdapter(mContext, Arrays.asList(getResources().getStringArray(R.array.categories)));

        this.spinner.setAdapter(adapter);

        return view;
    }

    @Override
    public boolean onLongClick(final View v)
    {

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if(v.getId() == picButton.getId())
                        {
                            setPicture(null);
                        }
                        else if (v.getId() == micButton.getId())
                        {
                            setAudio(null);
                        }
                        else if (v.getId() == vidButton.getId())
                        {
                            setVideo(null);
                        }

                        restoreAlpha(-1);

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                }
            }
        };

        AlertDialog.Builder builder;
        AlertDialog dialog = null;

        if(v.getId() == micButton.getId())
        {
            if(this.audio == null) return true;

            builder  = new AlertDialog.Builder(getActivity());
            dialog = builder.setMessage("Discard recording?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        else if(v.getId() == picButton.getId())
        {
            if(this.pic == null) return true;

            builder  = new AlertDialog.Builder(getActivity());
            dialog = builder.setMessage("Discard picture?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        else if(v.getId() == vidButton.getId())
        {
            if(this.video == null) return true;

            builder  = new AlertDialog.Builder(getActivity());
            dialog = builder.setMessage("Discard video?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        if(dialog == null) return false;

        TextView dialogText = (TextView)dialog.findViewById(android.R.id.message);
        dialogText.setGravity(Gravity.CENTER);
        dialog.show();

        return true;

    }

    /**
     * Checks if all sharing constraints are satisfied. This method also shows Toasts if constraints are not satisfied
     * @return true if it is possible to share
     */

    private boolean canShare(Location current_location){
        //if there is no content
        if(this.textView.getText().toString().length() == 0 && !isPicTaken && !isVideoShoot && !isSoundCaptured)
        {
            Map<String, String> dimensions = new HashMap<>(1);
            dimensions.put("reason", "Share without any content");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);

            this.onShareFailed(EMPTY_FLAG_TEXT);
            return false;
        }
        else if(current_location == null){

            Log.d(TAG, "No GPS data");
            Map<String, String> dimensions = new HashMap<>(1);
            dimensions.put("reason", "Share with No GPS");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            this.onShareFailed(ENABLE_NETWORK_SERVICE_TEXT);
            return false;
        }
        else if(!FacebookUtils.getInstance().hasCurrentUserId()){

            Map<String, String> dimensions = new HashMap<>(1);
            dimensions.put("reason", "Share with No Facebook");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            this.onShareFailed(FB_ID_NOT_FOUND_TEXT);
            return false;
        }

        return true;
    }


    private void share()
    {
        Location current_location =  PlacesApplication.getInstance().getLocation();
        if(PlacesApplication.isRunningOnEmulator){
            current_location = LocationService.getRandomLocation(current_location, 100);
            Log.d(TAG, "Generata Posizione casuale per simulatore: "+current_location);
        }

        if(!this.canShare(current_location)){
            return;
        }

        final Flag f = new Flag();

        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        final String category = spinner.getSelectedItem().toString();

        f.setFbId(FacebookUtils.getInstance().getCurrentUserId());
        FacebookUtils.getInstance().getFacebookUsernameFromID(FacebookUtils.getInstance().getCurrentUserId(), new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                f.setFbName(result);
            }
        });
        f.setCategory(category);
        f.setLocation(p);
        f.setText(this.textView.getText().toString());
        f.setWeather(PlacesApplication.getInstance().getWeather());

        FlagUploader uploader = new FlagUploader(f, mContext);
        //uploader.setDeletesFilesOnFinish(true);

        try{
            if( isPicTaken && this.pic != null){
                Log.v(TAG, "Successfully retrieved pic.");
                //ParseFile parse_pic = new ParseFile(this.pic.getName(), Utils.convertFileToByteArray(this.pic));
                uploader.setPictureFile(this.pic);
                //f.setPictureFile(parse_pic);
            }
            else if( isPicTaken ){ // equals isPicTaken && pic == null)
                Toast.makeText(mContext, PIC_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }

            if(isSoundCaptured && this.audio != null){
                Log.v(TAG, "Successfully retrieved recording.");
                //ParseFile parse_audio = new ParseFile(this.audio.getName(), Utils.convertFileToByteArray(this.audio));
                uploader.setAudioFile(this.audio);
                //f.setAudioFile(parse_audio);
            }
            else if(isSoundCaptured){ //equals isSoundCaptured && audio == null
                Toast.makeText(mContext, AUDIO_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }

            if (isVideoShoot && video != null){
                Log.v(TAG, "Successfully retrieved video.");
                //ParseFile parse_video = new ParseFile(this.video.getName(), Utils.convertFileToByteArray(this.video));
                uploader.setVideoFile(this.video);
                //f.setVideoFile(parse_video);
            }
            else if(isVideoShoot){
                Toast.makeText(mContext, VIDEO_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }
        }
        catch (Exception e){
            Log.d(TAG, "Error", e);
            return;
        }


        AlphaAnimation inAnim = new AlphaAnimation(0, 1);
        inAnim.setDuration(ANIMATION_DURATION);
        progressBarHolder.setAnimation(inAnim);
        progressBarHolder.setVisibility(View.VISIBLE);

        uploader.upload(new FlagUploader.FlagUploaderCallbacks() {
            @Override
            public void onPercentage(int percentage, String text_to_show) {
                progressTextView.setText(text_to_show+' '+percentage+'%');
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, e.getMessage());
                Map<String, String> dimensions = new HashMap<>(1);
                dimensions.put("reason", e.getMessage());
                ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
                onShareSucceeded(e.getMessage());
                this.dismissProgressBar();
            }

            @Override
            public void onSuccess()
            {
                Log.d(TAG, "Success");
                // ((com.gcw.sapienza.places.MainActivity) getActivity()).refresh();
                Map<String, String> dimensions = new HashMap<>(1);
                dimensions.put("category", category);
                ParseAnalytics.trackEventInBackground("sharing_succeeded", dimensions);

                onShareSucceeded(FLAG_PLACED_TEXT);
                this.dismissProgressBar();
            }

            void dismissProgressBar(){
                AlphaAnimation outAnim = new AlphaAnimation(1, 0);
                outAnim.setDuration(ANIMATION_DURATION);
                progressBarHolder.setAnimation(outAnim);
                progressBarHolder.setVisibility(View.GONE);
            }
        });

        this.resetMedia();

    }



    protected void resetMedia()
    {
        this.setAudio(null);
        this.setPicture(null);
        this.setVideo(null);

        Log.v(TAG, "Media has been cleared!");
    }

    protected void onShareFailed(String toastText){
        Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show();
        this.shareButton.setClickable(true);
    }

    protected void onShareSucceeded(String toastText)
    {
        /*
        this.textView.setText("");

        this.spinner.setSelection(0);

        this.resetMedia();
        this.hideKeyboard();

        Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show();

        this.shareButton.setClickable(true);*/
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", toastText);
        getActivity().setResult(Activity.RESULT_OK, returnIntent);
        getActivity().finish();

    }


    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null)
        {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }


    public void takePic(View v)
    {
        Vibrator vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        restoreAlpha(PIC_CODE);

        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePicture.resolveActivity(mContext.getPackageManager()) != null){
            this.imageFile = null;
            try{
                this.imageFile = Utils.createImageFile(ShareFragment.PICTURE_FORMAT);
            }
            catch (IOException e){
                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            if(this.imageFile != null){
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(this.imageFile));
                this.startActivityForResult(takePicture, Utils.PIC_CAPTURE_REQUEST_CODE);
            }
        }
    }


    public void shootVid(View v)
    {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        restoreAlpha(VIDEO_CODE);

        Intent videoIntent = new Intent(mContext, VideoCaptureActivity.class);

        if (videoIntent.resolveActivity(mContext.getPackageManager()) != null) {
            startActivityForResult(videoIntent, Utils.VID_SHOOT_REQUEST_CODE);
        }

    }


    @Override
    public void onPause()
    {
        super.onPause();

        MenuItem item = ((MainActivity2)getActivity()).mMenu.findItem(R.id.action_add_flag);
        item.setVisible(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == Utils.PIC_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK){

            if(this.imageFile == null || !this.imageFile.canRead()){
                Toast.makeText(mContext, "Error encountered while taking picture", Toast.LENGTH_LONG).show();
                Log.v(TAG, "Error encountered while taking picture");
                this.imageFile = null;
                return;
            }

            this.setPicture(this.imageFile.getAbsolutePath());
            this.imageFile = null;

            changeAlphaBasedOnSelection(PIC_CODE);
        }
        else if(requestCode == Utils.PIC_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
            Log.v(TAG, "Camera Intent canceled");
        }
        else if(requestCode ==  Utils.VID_SHOOT_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            String videoPath = data.getExtras().getString("result");
            this.setVideo(videoPath);

            changeAlphaBasedOnSelection(VIDEO_CODE);
        }
        else if(requestCode == Utils.VID_SHOOT_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
            Log.v(TAG, "Video Intent canceled");
        }
    }

    @Deprecated
    private static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Video.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void changeAlphaBasedOnSelection(int media_code)
    {
        switch(media_code)
        {
            case PIC_CODE:
                this.setVideo(null);
                this.setAudio(null);

                this.vidButton.setAlpha(0.5f);
                this.micButton.setAlpha(0.5f);

                break;

            case AUDIO_CODE:
                this.setPicture(null);
                this.setVideo(null);

                this.picButton.setAlpha(0.5f);
                this.vidButton.setAlpha(0.5f);

                break;

            case VIDEO_CODE:
                this.setPicture(null);
                this.setAudio(null);

                this.picButton.setAlpha(0.5f);
                this.micButton.setAlpha(0.5f);
        }
    }

    private void restoreAlpha(int media_code)
    {
        if(media_code == -1 || media_code == PIC_CODE) this.picButton.setAlpha(1f);
        if(media_code == -1 || media_code == AUDIO_CODE) this.micButton.setAlpha(1f);
        if(media_code == -1 || media_code == VIDEO_CODE) this.vidButton.setAlpha(1f);
    }

}
