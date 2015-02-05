package com.gcw.sapienza.places;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.activities.VideoCaptureActivity;
import com.gcw.sapienza.places.adapters.MSpinnerAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
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


public class ShareActivity extends ActionBarActivity implements View.OnLongClickListener, View.OnClickListener, View.OnTouchListener
{

    private static final String TAG = "ShareActivity";
    public static final String PICTURE_FORMAT = ".jpg";
    public static final String AUDIO_FORMAT = ".3gp";
    public static final String VIDEO_FORMAT = ".mp4";

    private static final int PIC_CODE = 0;
    private static final int AUDIO_CODE = 1;
    private static final int VIDEO_CODE = 2;
    private static final int PHONE_MEDIA_CODE = 3;


    private Spinner spinner;
    private TextView textView;
    private Button shareButton;
    private RelativeLayout progressBarHolder;
    private TextView progressTextView;

    private ImageButton picButton;
    private ImageButton micButton;
    private ImageButton vidButton;
    private ImageButton phoneButton;

    private boolean isPicTaken = false;
    private boolean isVideoShoot = false;
    private boolean isSoundCaptured = false;
    private boolean isPhoneMediaSelected = false;


    private File pic;
    private File video;
    private File audio;
    private File phoneMedia;

    protected static MediaRecorder audioRec;
    protected static String audio_filename;

    private File imageFile;

    private static final int ANIMATION_DURATION = 300;

    private static final String FLAG_PLACED_TEXT = "Flag has been placed!";
    private static final String FB_ID_NOT_FOUND_TEXT = "Couldn't retrieve your Facebook credentials\nPlease check your internet connection.";
    private static final String EMPTY_FLAG_TEXT = "Please insert text or take a picture";
    private static final String ENABLE_NETWORK_SERVICE_TEXT = "Please enable GPS/Network service";
    private static final String PIC_NOT_FOUND_TEXT = "Error encountered while retrieving picture\nFlag won't be stored";
    private static final String AUDIO_NOT_FOUND_TEXT = "Error encountered while retrieving recording\nFlag won't be stored";
    private static final String VIDEO_NOT_FOUND_TEXT = "Error encountered while retrieving video\nFlag won't be stored";
    private static final String ERROR_WHILE_RECORDING_TEXT = "Error encountered while recording";
    private static final String PHONE_MEDIA_NOT_FOUND_TEXT = "Error encountered while retrieving phone media\nFlag won't be stored";


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

    private void setPhoneMedia(String phoneMediaPath) {
        this.phoneMedia = null;
        if(phoneMediaPath != null)
        {
            File f = new File(phoneMediaPath);
            this.phoneMedia = f.canRead() ? f : null;
        }
        this.isPhoneMediaSelected = this.phoneMedia != null;

        if(this.phoneButton != null)
        {
            int res =R.drawable.attach_selector;
            if(this.isPhoneMediaSelected)
            {
                res = R.drawable.attach_taken;
            }

            this.phoneButton.setImageDrawable( getResources().getDrawable(res));
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

    public String getPhoneMediaPath(){
        return this.phoneMedia == null ? null : this.phoneMedia.getAbsolutePath();
    }

    private void handleIntent(){
        Intent intent = this.getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                this.handleShareText(intent);
            }
            else if (type.startsWith("image/")) {
                this.handleShareImage(intent);
            }
            else if(type.startsWith("video/")){
                this.handleShareVideo(intent);
            }


            if(FacebookUtils.isFacebookSessionOpened()){
                FacebookUtils.downloadFacebookInfo(this);
            }
            else{
                FacebookUtils.startLoginActivity(this);
            }

        }


    }


    private void handleShareImage(Intent intent){
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            File imageFile = new File(Utils.getImageRealPathFromURI(this, imageUri));
            if(imageFile.length() >= Flag.MAX_FILE_SIZE_BYTES){
                AlertDialog.Builder builder  = new AlertDialog.Builder(this);
                builder.setMessage("Cannot share this picture :(\nPlease, choose a smaller one").setNegativeButton("No", null).show();
            }
            else{
                this.setPhoneMedia(imageFile.getAbsolutePath());
                this.changeAlphaBasedOnSelection(PHONE_MEDIA_CODE);
            }

        }
    }

    private void handleShareText(Intent intent){
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if(text != null){
            this.textView.setText(text);

        }

    }

    private void handleShareVideo(Intent intent){
        Uri videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (videoUri != null) {
            File videoFile = new File(Utils.getVideoRealPathFromURI(this, videoUri));
            if(videoFile.length() >= Flag.MAX_FILE_SIZE_BYTES){
                AlertDialog.Builder builder  = new AlertDialog.Builder(this);
                builder.setMessage("Cannot share this video :(\nPlease, choose a smaller one").setNegativeButton("No", null).show();
            }
            else{
                this.setPhoneMedia(videoFile.getAbsolutePath());
                this.changeAlphaBasedOnSelection(PHONE_MEDIA_CODE);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_share);

        this.progressBarHolder = (RelativeLayout)findViewById(R.id.frame_layout);
        this.progressTextView = (TextView)findViewById(R.id.share_progress_text_view);

        this.picButton = (ImageButton)findViewById(R.id.pic_button);
        this.micButton = (ImageButton)findViewById(R.id.mic_button);
        this.vidButton = (ImageButton)findViewById(R.id.vid_button);
        this.phoneButton = (ImageButton)findViewById(R.id.phone_button);
        this.getSupportActionBar().setTitle("Places");

        //these lines are necessary for a correct visualization
        this.setPicture(this.getPicPath());
        this.setVideo(this.getVideoPath());
        this.setAudio(this.getAudioPath());
        this.setPhoneMedia(this.getPhoneMediaPath());

        this.picButton.setOnLongClickListener(this);
        this.micButton.setOnLongClickListener(this);
        this.vidButton.setOnLongClickListener(this);
        this.phoneButton.setOnLongClickListener(this);

        this.phoneButton.setOnClickListener(this);
        this.picButton.setOnClickListener(this);
        this.vidButton.setOnClickListener(this);

        this.micButton.setOnTouchListener(this);

        this.textView = (TextView)findViewById(R.id.share_text_field);
        this.textView.setGravity(Gravity.CENTER);

        this.shareButton = (Button)findViewById(R.id.share_button);
        this.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);
                ShareActivity.this.share();

            }
        });

        this.spinner = (Spinner)findViewById(R.id.spinner);

        // ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.categories, R.layout.custom_spinner);
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        MSpinnerAdapter adapter = new MSpinnerAdapter(this, Arrays.asList(getResources().getStringArray(R.array.categories)));

        this.spinner.setAdapter(adapter);

        this.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00884a")));

        this.handleIntent();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
        if(ShareActivity.this.isSoundCaptured)
        {
            return false;
        }

        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {

            restoreAlpha(AUDIO_CODE);

            try
            {
                audio_filename = Utils.createAudioFile(ShareActivity.AUDIO_FORMAT, ShareActivity.this).getAbsolutePath(); //Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp";
                audioRec = new MediaRecorder();
                audioRec.setAudioSource(MediaRecorder.AudioSource.MIC);
                audioRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                audioRec.setOutputFile(audio_filename);
                audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                audioRec.prepare();
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
                Toast.makeText(this, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
                Log.e(TAG, ERROR_WHILE_RECORDING_TEXT);
            }
            audioRec.start();
        }
        else if(event.getAction() == MotionEvent.ACTION_UP)
        {
            if(audioRec == null)
            {
                Toast.makeText(this, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, ERROR_WHILE_RECORDING_TEXT, Toast.LENGTH_LONG).show();
                return true;
            }
            ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.mic_green_taken));
            File audio_file = new File(audio_filename);
            try
            {
                FileInputStream inStream = new FileInputStream(audio_file);
                ShareActivity.this.setAudio(audio_filename);

                changeAlphaBasedOnSelection(AUDIO_CODE);

                inStream.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.vid_button) shootVid(v);
        else if(v.getId() == R.id.pic_button) takePic(v);
        else if(v.getId() == R.id.phone_button) getMedia(v);
    }

    @Override
    public boolean onLongClick(final View v)
    {

        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if(v.getId() == ShareActivity.this.picButton.getId())
                        {
                            ShareActivity.this.setPicture(null);
                        }
                        else if (v.getId() == ShareActivity.this.micButton.getId())
                        {
                            ShareActivity.this.setAudio(null);
                        }
                        else if (v.getId() == ShareActivity.this.vidButton.getId())
                        {
                            ShareActivity.this.setVideo(null);
                        }
                        else if(v.getId() == ShareActivity.this.phoneButton.getId())
                        {
                            setPhoneMedia(null);
                        }

                        restoreAlpha(-1);

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                }
            }
        };

        AlertDialog.Builder builder;
        AlertDialog dialog = null;

        if(v.getId() == ShareActivity.this.micButton.getId())
        {
            if(this.audio == null) return true;

            builder  = new AlertDialog.Builder(this);
            dialog = builder.setMessage("Discard recording?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        else if(v.getId() == ShareActivity.this.picButton.getId())
        {
            if(this.pic == null) return true;

            builder  = new AlertDialog.Builder(this);
            dialog = builder.setMessage("Discard picture?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        else if(v.getId() == ShareActivity.this.vidButton.getId())
        {
            if(this.video == null) return true;

            builder  = new AlertDialog.Builder(this);
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
        if(this.textView.getText().toString().length() == 0 && !isPicTaken
                && !isVideoShoot && !isSoundCaptured && !isPhoneMediaSelected)
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

        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        final String category = spinner.getSelectedItem().toString();

        f.setFbId(FacebookUtils.getInstance().getCurrentUserId());
        f.setCategory(category);
        f.setLocation(p);
        f.setText(this.textView.getText().toString());
        f.setWeather(PlacesApplication.getInstance().getWeather());

        FlagUploader uploader = new FlagUploader(f, this);
        //uploader.setDeletesFilesOnFinish(true);

        try{
            if( isPicTaken && this.pic != null){
                Log.v(TAG, "Successfully retrieved pic.");
                //ParseFile parse_pic = new ParseFile(this.pic.getName(), Utils.convertFileToByteArray(this.pic));
                uploader.setPictureFile(this.pic);
                //f.setPictureFile(parse_pic);
            }
            else if( isPicTaken ){ // equals isPicTaken && pic == null)
                Toast.makeText(this, PIC_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }

            if(isSoundCaptured && this.audio != null){
                Log.v(TAG, "Successfully retrieved recording.");
                //ParseFile parse_audio = new ParseFile(this.audio.getName(), Utils.convertFileToByteArray(this.audio));
                uploader.setAudioFile(this.audio);
                //f.setAudioFile(parse_audio);
            }
            else if(isSoundCaptured){ //equals isSoundCaptured && audio == null
                Toast.makeText(this, AUDIO_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }

            if (isVideoShoot && video != null){
                Log.v(TAG, "Successfully retrieved video.");
                //ParseFile parse_video = new ParseFile(this.video.getName(), Utils.convertFileToByteArray(this.video));
                uploader.setVideoFile(this.video);
                //f.setVideoFile(parse_video);
            }
            else if(isVideoShoot){
                Toast.makeText(this, VIDEO_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
                return;
            }
            if( isPhoneMediaSelected && this.phoneMedia != null){
                Log.v(TAG, "Successfully retrieved media.");
                //ParseFile parse_pic = new ParseFile(this.pic.getName(), Utils.convertFileToByteArray(this.pic));
                uploader.setPhoneMediaFile(this.phoneMedia);
                //f.setPictureFile(parse_pic);
            }
            else if( isPhoneMediaSelected ){ // equals isPicTaken && pic == null)
                Toast.makeText(this, PHONE_MEDIA_NOT_FOUND_TEXT, Toast.LENGTH_LONG).show();
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
                ShareActivity.this.progressTextView.setText(text_to_show+' '+percentage+'%');
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
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
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
        setResult(RESULT_OK,returnIntent);
        this.finish();

    }

/*
    public void hideKeyboard()
    {
        if(this.getCurrentFocus()!=null)
        {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
    }
*/

    public void takePic(View v)
    {
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        restoreAlpha(PIC_CODE);

        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePicture.resolveActivity(this.getPackageManager()) != null){
            this.imageFile = null;
            try{
                this.imageFile = Utils.createImageFile(ShareActivity.PICTURE_FORMAT);
            }
            catch (IOException e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            if(this.imageFile != null){
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(this.imageFile));
                this.startActivityForResult(takePicture, Utils.PIC_CAPTURE_REQUEST_CODE);
            }
        }
    }


    public void shootVid(View v)
    {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        restoreAlpha(VIDEO_CODE);

        Intent videoIntent = new Intent(this, VideoCaptureActivity.class);

        if (videoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(videoIntent, Utils.VID_SHOOT_REQUEST_CODE);
        }

    }

    private void getMedia(View v) {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        restoreAlpha(PHONE_MEDIA_CODE);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    Utils.PHONE_MEDIA_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // no file manager installed
            Log.e(TAG, ex.getMessage());
            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Utils.LOGIN_REQUEST_CODE && resultCode == RESULT_OK){
            FacebookUtils.downloadFacebookInfo(this);
        }
        else if(requestCode == Utils.PIC_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK){

            if(this.imageFile == null || !this.imageFile.canRead()){
                Toast.makeText(getApplicationContext(), "Error encountered while taking picture", Toast.LENGTH_LONG).show();
                Log.v(TAG, "Error encountered while taking picture");
                this.imageFile = null;
                return;
            }

            this.setPicture(this.imageFile.getAbsolutePath());
            this.imageFile = null;

            changeAlphaBasedOnSelection(PIC_CODE);
        }
        else if(requestCode == Utils.PIC_CAPTURE_REQUEST_CODE && resultCode == RESULT_CANCELED){
            Log.v(TAG, "Camera Intent canceled");
        }
        else if(requestCode ==  Utils.VID_SHOOT_REQUEST_CODE && resultCode == RESULT_OK){
            String videoPath = data.getExtras().getString("result");
            this.setVideo(videoPath);

            changeAlphaBasedOnSelection(VIDEO_CODE);
        }
        else if(requestCode == Utils.VID_SHOOT_REQUEST_CODE && resultCode == RESULT_CANCELED){
            Log.v(TAG, "Video Intent canceled");
        }
        else if(requestCode ==  Utils.PHONE_MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri mediaUri = data.getData();
            String mediaPath = getPath(this, mediaUri);
            if(new File(mediaPath) != null) {

                this.setPhoneMedia(mediaPath);

                changeAlphaBasedOnSelection(PHONE_MEDIA_CODE);

                Log.d(TAG, "Media Path selected: " + mediaPath);
                Log.d(TAG, "Media Path selected(Uri): " + mediaUri.getPath());

            }else{
                Toast.makeText(this, "Invalid Media Selected", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == Utils.PHONE_MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
            Log.v(TAG, "Phone Media Intent canceled");
        }
    }

    //from the open source library aFileChooser: https://github.com/iPaulPro/aFileChooser
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private void changeAlphaBasedOnSelection(int media_code)
    {
        switch(media_code)
        {
            case PIC_CODE:
                this.setVideo(null);
                this.setAudio(null);
                this.setPhoneMedia(null);

                this.vidButton.setAlpha(0.5f);
                this.micButton.setAlpha(0.5f);
                this.phoneButton.setAlpha(0.5f);

                this.phoneButton.setEnabled(false);
                this.vidButton.setEnabled(false);
                this.micButton.setEnabled(false);

                break;

            case AUDIO_CODE:
                this.setPicture(null);
                this.setVideo(null);
                this.setPhoneMedia(null);

                this.picButton.setAlpha(0.5f);
                this.vidButton.setAlpha(0.5f);
                this.phoneButton.setAlpha(0.5f);

                this.phoneButton.setEnabled(false);
                this.micButton.setEnabled(false);
                this.vidButton.setEnabled(false);

                break;

            case VIDEO_CODE:
                this.setPicture(null);
                this.setAudio(null);
                this.setPhoneMedia(null);

                this.picButton.setAlpha(0.5f);
                this.micButton.setAlpha(0.5f);
                this.phoneButton.setAlpha(0.5f);

                this.phoneButton.setEnabled(false);
                this.micButton.setEnabled(false);
                this.micButton.setEnabled(false);

                break;

            case PHONE_MEDIA_CODE:
                this.setPicture(null);
                this.setAudio(null);
                this.setVideo(null);

                this.picButton.setAlpha(0.5f);
                this.micButton.setAlpha(0.5f);
                this.vidButton.setAlpha(0.5f);

                this.micButton.setEnabled(false);
                this.vidButton.setEnabled(false);
                this.micButton.setEnabled(false);

                break;
        }
    }

    private void restoreAlpha(int media_code)
    {
        if(media_code == -1 || media_code == PIC_CODE){
            this.picButton.setAlpha(1f);
            this.picButton.setEnabled(true);
        }
        if(media_code == -1 || media_code == AUDIO_CODE){
            this.micButton.setAlpha(1f);
            this.micButton.setEnabled(true);
        }
        if(media_code == -1 || media_code == VIDEO_CODE){
            this.vidButton.setAlpha(1f);
            this.vidButton.setEnabled(true);
        }
        if(media_code == -1 || media_code == PHONE_MEDIA_CODE){
            this.phoneButton.setAlpha(1f);
            this.phoneButton.setEnabled(true);
        }
    }




}