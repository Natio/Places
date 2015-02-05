package com.gcw.sapienza.places.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by paolo on 29/01/15.
 * Class that records video :)
 */
public class VideoCaptureActivity extends Activity implements View.OnClickListener, MediaRecorder.OnInfoListener{

    private static final String TAG = "VideoCaptureActivity";

    private static final int MAX_VIDEO_LENGTH = 60000;
    private static final int MAX_VIDEO_LENGTH_HD = 24000;

    private static final int VIDEO_FPS = 20;
    private static final int VIDEO_FPS_HD = 24;


    private SurfaceHolder previewHolder;
    private ToggleButton toggleButton;
    private ToggleButton hdButton;
    private TextView timeTextView;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private File filePath;
    private Timer uiUpdateTimer;
    private long video_start_millis;
    private boolean cameraConfigured = false;
    private boolean inPreview=false;
    private boolean isRecording = false;
    private boolean hdEnabled = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.video_capture_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        this.timeTextView = (TextView) this.findViewById(R.id.timer_textView);
        this.timeTextView.setTextColor(Color.WHITE);
        this.timeTextView.setText(Integer.toString(MAX_VIDEO_LENGTH/1000));
        SurfaceView surface = (SurfaceView) this.findViewById(R.id.surfaceView);
        this.previewHolder = surface.getHolder();
        this.previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.previewHolder.addCallback(this.surfaceCallback);
        //this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.toggleButton = (ToggleButton) this.findViewById(R.id.toggleRecordingButton);
        this.toggleButton.setOnClickListener(this);
        this.toggleButton.setText("REC");
        this.toggleButton.setTextOff("REC");
        this.toggleButton.setTextOn("STOP");
        this.hdButton = (ToggleButton) this.findViewById(R.id.toggleHDButton);
        this.hdButton.setOnClickListener(this);
        this.hdButton.setText("HD");
        this.hdButton.setTextOff("HD");
        this.hdButton.setTextOn("HD");


    }

    @Override
    public void onResume() {
        super.onResume();

        camera=Camera.open();
        startPreview();
        if(this.isRecording){
            this.finishVideoCapture();
        }
        Log.d(TAG, camera.getParameters().flatten());
        if(!isHdAvailable()){
            this.hdButton.setVisibility(View.GONE);
        }
    }

    private boolean isHdAvailable() {
        try {
            Log.d(TAG, Arrays.asList(camera.getParameters().getSupportedPreviewFpsRange()).toString());
            return camera.getParameters().getSupportedVideoSizes().contains(camera.new Size(1280, 720))
                    && isSupportedFrameRate(VIDEO_FPS_HD);
        }catch(NullPointerException e){
            Log.e(TAG, "camera is null");
            e.printStackTrace();
            return false;
        }
    }

    private boolean isSupportedFrameRate(int videoFpsHd) {
        List<int[]> supportedFps = camera.getParameters().getSupportedPreviewFpsRange();
        for(int [] i: supportedFps){
            if(i[1] >= videoFpsHd * 1000)
                return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera=null;
        inPreview=false;

        super.onPause();
    }

    @Override
    public void onClick(View v){
        if(v.equals(toggleButton)){
            if (((ToggleButton)v).isChecked()) {
                this.startRecordingVideo();
                hdButton.setClickable(false);
            }
            else {
                this.finishVideoCapture();
                hdButton.setClickable(true);
            }
        }else if(v.equals(hdButton)){
            if (((ToggleButton)v).isChecked()) {
                enableHd();
            }
            else{
                disableHd();
            }
        }

    }

    private boolean isHdEnabled(){
        return hdEnabled;
    }

    private void enableHd() {
        hdEnabled = true;
    }

    private void disableHd() {
        hdEnabled = false;
    }


    @Override
    public void onInfo(MediaRecorder mr, int what, int extra){
        if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
            this.finishVideoCapture();
        }
        Log.d(TAG,"W: "+what);
    }





    private void startRecordingVideo(){
        this.isRecording = true;
        this.setupRecorder();
        this.mediaRecorder.start();
        this.uiUpdateTimer = new Timer();
        this.video_start_millis = System.currentTimeMillis();
        this.uiUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // VideoCaptureActivity.this.timeTextView.setText(Integer.toString(VideoCaptureActivity.this.current_video_length));
                VideoCaptureActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        long diff = System.currentTimeMillis() - VideoCaptureActivity.this.video_start_millis;
                        long seconds = ((hdEnabled ? MAX_VIDEO_LENGTH_HD : MAX_VIDEO_LENGTH) - diff) / 1000;
                        VideoCaptureActivity.this.timeTextView.setText(Long.toString(seconds));
                    }
                });
            }
        }, 0, 500);
    }

    private void finishVideoCapture(){
        this.isRecording = false;
        this.uiUpdateTimer.cancel();
        this.uiUpdateTimer.purge();
        this.mediaRecorder.stop();
        this.mediaRecorder.reset();






        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent closeIntent = new Intent();
                        closeIntent.putExtra("result", VideoCaptureActivity.this.filePath.getAbsolutePath());
                        VideoCaptureActivity.this.setResult(RESULT_OK, closeIntent);


                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                }
                VideoCaptureActivity.this.finish();
            }
        };

        AlertDialog.Builder builder  = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to use this video ?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    private void setupRecorder(){

        int camera_rotation = this.lockAndReturnRightCameraRotation(true);

        if (inPreview) {
            camera.stopPreview();
            try{
                camera.setPreviewDisplay(null);
            }catch (Throwable t) {
                Log.e(TAG,"Exception in setPreviewDisplay() in setupRecorder", t);
                Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
            camera.unlock();
        }


        this.mediaRecorder = new MediaRecorder();
        this.mediaRecorder.setOnInfoListener(this);
        this.mediaRecorder.setCamera(this.camera);
        this.mediaRecorder.setPreviewDisplay(this.previewHolder.getSurface());


        this.mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        this.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        this.mediaRecorder.setAudioEncodingBitRate(96000);
        this.mediaRecorder.setVideoEncodingBitRate(1229550); //1229.55kbps


        this.mediaRecorder.setMaxDuration(hdEnabled ? MAX_VIDEO_LENGTH_HD : MAX_VIDEO_LENGTH);


        try{
            this.filePath = Utils.createRecordingVideoFile(".mp4");
        }
        catch (IOException e){
            Log.d(TAG, "Error creating file", e);
            return;
        }

        this.mediaRecorder.setOutputFile(this.filePath.getAbsolutePath());
        this.mediaRecorder.setVideoFrameRate(hdEnabled ? VIDEO_FPS_HD : VIDEO_FPS);
        this.mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        this.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        if(hdEnabled)
            this.mediaRecorder.setVideoSize(1280,720);
        else
            this.mediaRecorder.setVideoSize(640, 480);

        this.mediaRecorder.setOrientationHint(camera_rotation);


        try {
            this.mediaRecorder.prepare();
        } catch (IOException e) {
            // This is thrown if the previous calls are not called with the
            // proper order
            e.printStackTrace();
        }

    }

    private int lockAndReturnRightCameraRotation(boolean lock){
        int orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        int rotation = 0;
        switch(this.getWindowManager().getDefaultDisplay().getRotation()){
            case Surface.ROTATION_0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                rotation = 90;
                break;
            case Surface.ROTATION_90:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                rotation = 0;
                break;
            case Surface.ROTATION_180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                rotation = 90;
                break;
            case Surface.ROTATION_270:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                rotation = 180;
                break;
        }
        if(lock){
            setRequestedOrientation(orientation);
        }

        return rotation;
    }

    private void shutdown() {
        // Release MediaRecorder and especially the Camera as it's a shared
        // object that can be used by other applications
        if(this.mediaRecorder != null){
            this.mediaRecorder.reset();
            this.mediaRecorder.release();
            this.mediaRecorder = null;
        }

        if(this.camera != null){
            this.camera.release();
            this.camera = null;
        }


        this.filePath = null;
    }



    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);

            }
            catch (Throwable t) {
                Log.e(TAG,"Exception in setPreviewDisplay()", t);
                Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,
                        parameters);
                this.camera.setDisplayOrientation(this.lockAndReturnRightCameraRotation(false));
                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);

                    cameraConfigured=true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.startPreview();
            inPreview=true;
        }
    }


    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            initPreview(width, height);
            startPreview();
            Log.d(TAG, "surfaceChanged");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            shutdown();
        }
    };

}
