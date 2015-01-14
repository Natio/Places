package com.gcw.sapienza.places;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Created by mic_head on 01/01/15.
 */
public class FlagActivity extends Activity {

    private String text;
    private String id;
    private String date;
    private String weather;
    private String category;

    private String pic_path;
    private String audio_path;
    private String video_path;

    private MediaPlayer mediaPlayer;

    private VideoView vv;

    private static final String TAG = "FlagActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.flag_layout);

        Bundle bundle = getIntent().getExtras();

        text = bundle.getString("text");
        id = bundle.getString("id");
        date = bundle.getString("date");
        weather = bundle.getString("weather");
        category = bundle.getString("category");

        ImageView iw = ((ImageView)findViewById(R.id.pic));

        vv = (VideoView)findViewById(R.id.vid);
        this.pic_path = bundle.getString("picture");
        if(this.pic_path != null)
        {
            //pic = new byte[bundle.getByteArray("picture").length];
            //System.arraycopy(bundle.getByteArray("picture"), 0, pic, 0, pic.length);
            iw.setImageBitmap(BitmapFactory.decodeFile(this.pic_path));

        }
        else
        {
            iw.setMaxHeight(0);
            iw.setMaxWidth(0);
        }
        this.audio_path = bundle.getString("audio");
        if(this.audio_path != null)
        {
            //audio = new byte[bundle.getByteArray("audio").length];
            //System.arraycopy(bundle.getByteArray("audio"), 0, audio, 0, audio.length);

            playRecording(this.audio_path);
        }

        if(bundle.getString("video") != null)
        {
            video_path = bundle.getString("video");

            playVideo(video_path);
        }
        else vv.setVisibility(View.INVISIBLE);

        ((EditText)findViewById(R.id.text)).setText(text);
        final String weatherString = (weather == null || weather.equals("")) ? "" : ", " + weather;

        final String bottomLineText = ", " + date + weatherString + "\nCategory: " + category;
        final TextView authorTextView = (TextView)findViewById(R.id.author);
        FacebookUtils.getInstance().getFacebookUsernameFromID(this.id, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                authorTextView.setText(result+bottomLineText);
            }
        });


        ImageView profilePicimageView = (ImageView)findViewById(R.id.profile_pic);

        FacebookUtils.getInstance().loadProfilePicIntoImageView(this.id, profilePicimageView, FacebookUtils.PicSize.LARGE);

    }

    private void playRecording(String audio_path)
    {
        try {
            File temp = new File(audio_path);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    mp.release();
                }
            });

            FileInputStream inStream = new FileInputStream(temp);
            mediaPlayer.setDataSource(inStream.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    private void playVideo(String video_path)
    {
        Uri videoUri = Uri.parse(video_path);

        if(videoUri != null)
        {
            vv.setVideoURI(videoUri);
            vv.start();
        }
    }

    @Deprecated
    private void loadProfilePictureFromUrl(String url){
        Picasso.with(this.getApplicationContext()).load(url).into((ImageView)findViewById(R.id.profile_pic));
    }

    @Deprecated //use loadProfilePictureFromUrl
    protected void streamProfilePic(final String image_url){
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(image_url).openConnection().getInputStream());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) findViewById(R.id.profile_pic)).setImageBitmap(bitmap);
                        }
                    });
                }
                catch (IOException ioe){ ioe.printStackTrace(); }
            }
        }).start();
    }
}
