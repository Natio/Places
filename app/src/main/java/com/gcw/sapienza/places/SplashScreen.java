package com.gcw.sapienza.places;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Simone on 12/12/2014.
 */
public class SplashScreen extends Activity
{
    public static final int TIMEOUT = 3000;

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
        startActivity(new Intent(SplashScreen.this, MainActivity.class));
        finish();
    }
}
