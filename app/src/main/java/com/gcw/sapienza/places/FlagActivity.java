package com.gcw.sapienza.places;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
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
    private String date;
    private byte[] pic;

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

        if(!Utils.userIdMap.containsKey(id))
        {
            Utils.fetchFbUsername(id);

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userIdMap.containsKey(id)) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else ((TextView)findViewById(R.id.author)).setText(Utils.userIdMap.get(id) + ", " + date);
                }
            });
        }
        else ((TextView)findViewById(R.id.author)).setText(Utils.userIdMap.get(id) + ", " + date);

        if(!Utils.userProfilePicMapLarge.containsKey(id))
        {
            try
            {
                Utils.fetchFbProfilePic(id, Utils.LARGE_PIC_SIZE);
            }
            catch(MalformedURLException mue){ mue.printStackTrace(); }
            catch (IOException ioe){ ioe.printStackTrace(); }

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.userProfilePicMapLarge.containsKey(id)) handler.postDelayed(this, Utils.UPDATE_DELAY);
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
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(Utils.userProfilePicMapLarge.get(id)).openConnection().getInputStream());

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
