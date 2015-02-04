package com.gcw.sapienza.places;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
public class FlagFragment extends Fragment implements View.OnClickListener {

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
    private RelativeLayout frameLayout;

    private ImageView focused_iw;

    private enum MediaType{ PIC, AUDIO, VIDEO, NONE }
    private MediaType mediaType;

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
        if(pic_path != null )
        {
            mediaType = MediaType.PIC;
            return;
        }

        video_path = bundle.getString("video");
        if(video_path != null )
        {
            mediaType = MediaType.VIDEO;
            return;
        }

        audio_path = bundle.getString("audio");
        if(audio_path != null )
        {
            mediaType = MediaType.AUDIO;
            return;
        }

        mediaType = MediaType.NONE;
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
        frameLayout = (RelativeLayout)view.findViewById(R.id.frame_layout);

        iw.setOnClickListener(this);
        frameLayout.setOnClickListener(this);

        if(mediaType == MediaType.NONE || mediaType == MediaType.AUDIO)
        {
            iw.setVisibility(View.GONE);
            vv.setVisibility(View.GONE);

            if(mediaType == MediaType.AUDIO) playRecording(this.audio_path);
        }
        else if(mediaType == MediaType.PIC)
        {
            Bitmap bm = BitmapFactory.decodeFile(this.pic_path);

            vv.setVisibility(View.GONE);
            iw.setImageBitmap(bm);

            focused_iw = (ImageView)view.findViewById(R.id.focused_pic);
            focused_iw.setImageBitmap(bm);
        }
        else
        {
            iw.setVisibility(View.GONE);
            playVideo(video_path);
        }

        flagText.setText(text);

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

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.frame_layout)
        {
            frameLayout.setVisibility(View.GONE);
        }
        else if(v.getId() == R.id.pic)
        {
            frameLayout.setVisibility(View.VISIBLE);
        }
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

