package com.gcw.sapienza.places.legacy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.gcw.sapienza.places.MainActivity;
import com.gcw.sapienza.places.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Simone on 12/12/2014.
 */
public class SplashScreenActivity extends FragmentActivity
{
    public static final int TIMEOUT = 1000;
    public static final String TAG = "SplashScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                goToMainScreen();
            }
        }, TIMEOUT);
    }

    private void goToMainScreen()
    {
        startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
        finish();
    }
}
