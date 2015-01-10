package com.gcw.sapienza.places;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.squareup.picasso.Picasso;

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
    private byte[] pic;
    private String weather;
    private String category;
    private String [] reports;

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

        if(bundle.getByteArray("pic") != null)
        {
            pic = new byte[bundle.getByteArray("pic").length];
            System.arraycopy(bundle.getByteArray("pic"), 0, pic, 0, pic.length);
            iw.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.length));
        }
        else
        {
            iw.setMaxHeight(0);
            iw.setMaxWidth(0);
        }

        ((EditText)findViewById(R.id.text)).setText(text);

        final String weatherString = (weather == null || weather.equals("")) ? "" : ", " + weather;

        String fb_user_id = FacebookUtils.getInstance().getUserNameFromId(this.id);

        if(fb_user_id == null)
        {
            FacebookUtils.getInstance().fetchFbUsername(this.id, new FacebookUtilCallback() {
                @Override
                public void onResult(String result, Exception e) {
                    if(e != null){
                        Log.d(TAG, e.getMessage());
                        return;
                    }
                    TextView author_tv = (TextView)findViewById(R.id.author);
                    String author_text = result + ", " + date + weatherString + "\nCategory: " + category;
                    author_tv.setText(author_text);
                }
            });

        }
        else{
            ((TextView)findViewById(R.id.author)).setText(fb_user_id+ ", " + date + weatherString + "\nCategory: " + category);
        }

        String pic_large_url = FacebookUtils.getInstance().getProfilePictureLarge(this.id);
        if(pic_large_url == null)
        {
            try
            {
                FacebookUtils.getInstance().fetchFbProfilePic(this.id, FacebookUtils.LARGE_PIC_SIZE, new FacebookUtilCallback() {
                    @Override
                    public void onResult(String result_url, Exception e) {
                        if(e != null){
                            Log.d(TAG, e.getMessage());
                            return;
                        }
                        //FlagActivity.this.streamProfilePic(result);
                        FlagActivity.this.loadProfilePictureFromUrl(result_url);
                    }
                });

            }
            catch(MalformedURLException mue){ mue.printStackTrace(); }
            catch (IOException ioe){ ioe.printStackTrace(); }


        }
        else{
            this.loadProfilePictureFromUrl(pic_large_url);
        }
    }

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
                catch(MalformedURLException mue){ mue.printStackTrace(); }
                catch (IOException ioe){ ioe.printStackTrace(); }
            }
        }).start();
    }
}
