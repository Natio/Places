package com.gcw.sapienza.places;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
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

    private byte[] pic;
    private byte[] audio;

    private MediaPlayer mediaPlayer;

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

        if(bundle.getByteArray("picture") != null)
        {
            pic = new byte[bundle.getByteArray("picture").length];
            System.arraycopy(bundle.getByteArray("picture"), 0, pic, 0, pic.length);
            iw.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.length));
        }
        else
        {
            iw.setMaxHeight(0);
            iw.setMaxWidth(0);
        }

        if(bundle.getByteArray("audio") != null)
        {
            audio = new byte[bundle.getByteArray("audio").length];
            System.arraycopy(bundle.getByteArray("audio"), 0, audio, 0, audio.length);

            playRecording(audio);
        }

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

    private void playRecording(byte[] sound_array)
    {
        try {
            File temp = File.createTempFile("places_temp_audio", "3gp", getCacheDir());
            temp.deleteOnExit();

            FileOutputStream outStream = new FileOutputStream(temp);
            outStream.write(sound_array);
            outStream.close();

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
