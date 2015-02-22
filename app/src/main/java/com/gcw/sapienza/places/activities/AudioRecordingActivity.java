package com.gcw.sapienza.places.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by mic_head on 15/02/15.
 */
public class AudioRecordingActivity extends ActionBarActivity implements View.OnClickListener, View.OnTouchListener
{

    private static final String TAG = "AudioRecordingActivity";

    private ImageView bigMicButton;
    private ImageView confirmButton;
    private ImageView cancelButton;

    private TextView recordText;

    protected static MediaRecorder audioRec;
    protected static String audio_filename;

    private enum RecordState{ RECORD, PLAY, PAUSE }
    private RecordState recordState = RecordState.RECORD;

    private MediaPlayer mediaPlayer;

    private static final String ERROR_WHILE_RECORDING_TEXT = "Error encountered while recording";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.audio_recording_layout);

        getSupportActionBar().setTitle("Places");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));

        bigMicButton = (ImageView)findViewById(R.id.big_mic_button);
        confirmButton = (ImageView)findViewById(R.id.confirm_button);
        cancelButton = (ImageView)findViewById(R.id.cancel_button);

        recordText = (TextView)findViewById(R.id.record_text);

        bigMicButton.setOnClickListener(this);
        confirmButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        bigMicButton.setOnTouchListener(this);
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.big_mic_button && recordState == RecordState.PLAY) playRecording();
        else if(v.getId() == R.id.confirm_button) confirm();
        else if(v.getId() == R.id.cancel_button) cancel();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(v.getId() == R.id.big_mic_button && recordState == RecordState.RECORD)
        {
            recordAudio(event);

            return true;
        }

        return super.onTouchEvent(event);
    }

    private void recordAudio(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            bigMicButton.setImageResource(R.drawable.mic_darkgreen);

            try
            {
                audio_filename = Utils.createAudioFile(ShareActivity.AUDIO_FORMAT, AudioRecordingActivity.this).getAbsolutePath();
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
                return;
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
                return;
            }

            bigMicButton.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
            recordState = RecordState.PLAY;

            try
            {
                File temp = new File(audio_filename);

                mediaPlayer = new MediaPlayer();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp)
                    {
                        bigMicButton.setImageResource(R.drawable.play_button);
                        recordState = RecordState.PLAY;
                    }
                });

                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                FileInputStream inStream = new FileInputStream(temp);
                mediaPlayer.setDataSource(inStream.getFD());

                mediaPlayer.prepare();
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }

            recordText.setText("Audio has been recorded!");

            confirmButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
        }
    }

    private void playRecording()
    {
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            bigMicButton.setImageResource(R.drawable.play_button);
            recordState = RecordState.PLAY;
        }
        else
        {
            mediaPlayer.start();
            bigMicButton.setImageResource(R.drawable.pause_button);
            recordState = RecordState.PAUSE;
        }
    }

    private void confirm()
    {
        Intent closeIntent = new Intent();
        closeIntent.putExtra("result", AudioRecordingActivity.this.audio_filename);
        AudioRecordingActivity.this.setResult(RESULT_OK, closeIntent);
        AudioRecordingActivity.this.finish();
    }

    private void cancel()
    {
        bigMicButton.setImageResource(R.drawable.mic_green);
        recordState = RecordState.RECORD;

        recordText.setText("Hold to record");

        cancelButton.setVisibility(View.GONE);
        confirmButton.setVisibility(View.GONE);
    }
}
