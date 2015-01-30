package com.gcw.sapienza.places.activities;

import android.app.Activity;
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
import android.widget.ToggleButton;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by paolo on 29/01/15.
 * Class that records video :)
 */
public class VideoCaptureActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback, MediaRecorder.OnInfoListener{

    private static final String TAG = "VideoCaptureActivity";
    private static final int MAX_VIDEO_LENGTH = 60000;


    private SurfaceHolder surfaceHolder;
    private ToggleButton toggleButton;
    private TextView timeTextView;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private File filePath;
    private Timer uiUpdateTimer;
    private long video_start_millis;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.video_capture_activity);
        //TODO orientation now is fixed. It must be possible do take videos in landscape and in portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        this.timeTextView = (TextView) this.findViewById(R.id.timer_textView);
        this.timeTextView.setTextColor(Color.WHITE);
        this.timeTextView.setText(Integer.toString(MAX_VIDEO_LENGTH/1000));
        SurfaceView surface = (SurfaceView) this.findViewById(R.id.surfaceView);
        this.surfaceHolder = surface.getHolder();
        this.surfaceHolder.addCallback(this);
        //this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.toggleButton = (ToggleButton) this.findViewById(R.id.toggleRecordingButton);
        this.toggleButton.setOnClickListener(this);
        this.toggleButton.setTextOff("REC");

    }

    @Override
    public void onClick(View v){
        if (((ToggleButton)v).isChecked()) {
            this.startRecordingVideo();
        }
        else {
            this.finishVideoCapture();
        }
    }


    @Override
    public void onInfo(MediaRecorder mr, int what, int extra){
        if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
            this.finishVideoCapture();
        }
        Log.d(TAG,"W: "+what);
    }





    private void startRecordingVideo(){
        this.setupRecorder();
        this.toggleButton.setText("STOP");
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
                        long seconds = (MAX_VIDEO_LENGTH - diff) / 1000;
                        VideoCaptureActivity.this.timeTextView.setText(Long.toString(seconds));
                    }
                });
            }
        },0,500);
    }

    private void finishVideoCapture(){
        this.uiUpdateTimer.cancel();
        this.uiUpdateTimer.purge();
        this.mediaRecorder.stop();
        this.mediaRecorder.reset();

        Intent closeIntent = new Intent();
        closeIntent.putExtra("result", this.filePath.getAbsolutePath());
        Log.d(TAG, "Size: " + this.filePath.length());

        this.setResult(RESULT_OK, closeIntent);
        this.finish();
    }

    private void setupRecorder(){

        int camera_rotation = this.lockAndReturnRightCameraRotation();

        if(this.camera == null){
            this.camera = Camera.open();
            this.camera.setDisplayOrientation(camera_rotation);
            this.camera.unlock();
        }

        if(this.mediaRecorder == null){
            this.mediaRecorder = new MediaRecorder();
            this.mediaRecorder.setOnInfoListener(this);
        }
        this.mediaRecorder.setCamera(this.camera);
        this.mediaRecorder.setPreviewDisplay(this.surfaceHolder.getSurface());


        this.mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        this.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        this.mediaRecorder.setAudioEncodingBitRate(96000);
        this.mediaRecorder.setVideoEncodingBitRate(1229550); //1229.55kbps


        this.mediaRecorder.setMaxDuration(MAX_VIDEO_LENGTH);


        try{
           this.filePath = Utils.createRecordingVideoFile(".mp4");
        }
        catch (IOException e){
            Log.d(TAG, "Error creating file", e);
            return;
        }

        this.mediaRecorder.setOutputFile(this.filePath.getAbsolutePath());
        this.mediaRecorder.setVideoFrameRate(20);
        this.mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        this.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

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

    private int lockAndReturnRightCameraRotation(){
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
                rotation = 0;
                break;
        }
        setRequestedOrientation(orientation);
        return rotation;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        this.shutdown();
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


}
