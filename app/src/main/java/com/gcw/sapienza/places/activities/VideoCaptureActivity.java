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
import android.widget.Toast;
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
public class VideoCaptureActivity extends Activity implements View.OnClickListener, MediaRecorder.OnInfoListener{

    private static final String TAG = "VideoCaptureActivity";
    private static final int MAX_VIDEO_LENGTH = 60000;


    private SurfaceHolder previewHolder;
    private ToggleButton toggleButton;
    private TextView timeTextView;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private File filePath;
    private Timer uiUpdateTimer;
    private long video_start_millis;
    private boolean cameraConfigured = false;
    private boolean inPreview=false;
    private boolean isRecodring = false;


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
        this.toggleButton.setTextOff("REC");

    }

    @Override
    public void onResume() {
        super.onResume();

        camera=Camera.open();
        startPreview();
        if(this.isRecodring){
            this.finishVideoCapture();
        }

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
        this.isRecodring = true;
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
        }, 0, 500);
    }

    private void finishVideoCapture(){
        this.isRecodring = false;
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
