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

import com.gcw.sapienza.places.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by mic_head on 01/01/15.
 */
public class FlagActivity extends Activity {

    private String text;
    private String id;

    private static final String TAG = "FlagActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        text = bundle.getString("text");
        id = bundle.getString("id");

        setContentView(R.layout.flag_layout);

        ((EditText)findViewById(R.id.text)).setText(text);
        if(!Utils.userIdMap.containsKey(id))
        {
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userIdMap.containsKey(id)) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else ((TextView)findViewById(R.id.author)).setText("by " + Utils.userIdMap.get(id));
                }
            });
        }
        else ((TextView)findViewById(R.id.author)).setText("by " + Utils.userIdMap.get(id));

        if(!Utils.userProfilePicMap.containsKey(id))
        {
            try
            {
                Utils.fetchFbProfilePic(id);
            }
            catch(MalformedURLException mue){ mue.printStackTrace(); }
            catch (IOException ioe){ ioe.printStackTrace(); }

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userProfilePicMap.containsKey(id)) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else streamProfilePic();
                }
            });
        }
        else streamProfilePic();
    }

    protected void streamProfilePic()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(Utils.userProfilePicMap.get(id)).openConnection().getInputStream());

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
