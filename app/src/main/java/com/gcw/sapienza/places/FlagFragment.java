package com.gcw.sapienza.places;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Created by mic_head on 02/02/15.
 */
public class FlagFragment extends Fragment {

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
    private ImageView iw;
    private TextView authorTextView;
    private EditText flagText;
    private ImageView profilePicimageView;

    private View view;

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "FlagFragment";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        text = bundle.getString("text");
        id = bundle.getString("id");
        date = bundle.getString("date");
        weather = bundle.getString("weather");
        category = bundle.getString("category");
        pic_path = bundle.getString("picture");
        video_path = bundle.getString("video");
        audio_path = bundle.getString("audio");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.flag_layout, container, false);

        iw = ((ImageView)view.findViewById(R.id.pic));
        vv = (VideoView)view.findViewById(R.id.vid);
        flagText = (EditText)view.findViewById(R.id.text);
        authorTextView = (TextView)view.findViewById(R.id.author);
        profilePicimageView = (ImageView)view.findViewById(R.id.profile_pic);

        flagText.setText(text);

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

        if(this.audio_path != null)
        {
            //audio = new byte[bundle.getByteArray("audio").length];
            //System.arraycopy(bundle.getByteArray("audio"), 0, audio, 0, audio.length);

            playRecording(this.audio_path);
        }

        if(this.video_path != null)
        {
            playVideo(video_path);
        }
        else vv.setVisibility(View.INVISIBLE);

        final String weatherString = (weather == null || weather.isEmpty()) ? "" : "\nWeather: " + weather;
        final String bottomLineText = date + weatherString + "\nCategory: " + category;

        FacebookUtils.getInstance().getFacebookUsernameFromID(this.id, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                authorTextView.setText("Author: " + result + bottomLineText);
            }
        });

        FacebookUtils.getInstance().loadProfilePicIntoImageView(this.id, profilePicimageView, FacebookUtils.PicSize.LARGE);

        return view;
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
        if(video_path != null)
        {
            Uri videoUri = Uri.parse(video_path);
            vv.setVideoURI(videoUri);
            vv.start();
        }
    }

    @Deprecated
    private void loadProfilePictureFromUrl(String url){
        Picasso.with(getActivity().getApplicationContext()).load(url).into((ImageView)view.findViewById(R.id.profile_pic));
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

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) view.findViewById(R.id.profile_pic)).setImageBitmap(bitmap);
                        }
                    });
                }
                catch (IOException ioe){ ioe.printStackTrace(); }
            }
        }).start();
    }
}

