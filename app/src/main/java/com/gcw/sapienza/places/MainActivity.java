package com.gcw.sapienza.places;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.gcw.sapienza.places.utils.Utils;
import com.gcw.sapienza.places.services.LocationService;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener {

    private static final String TAG = "MainActivity";

    private static boolean isForeground = false;

    SectionsPagerAdapter mSectionsPagerAdapter;
    Fragment[] fragments = {new ShareFragment(), new MosaicFragment(), new MMapFragment()};
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        logRun();

        if(ParseFacebookUtils.getSession() == null || ParseFacebookUtils.getSession().isClosed())
        {
            ParseLoginBuilder builder = new ParseLoginBuilder(MainActivity.this);

            builder.setParseLoginEnabled(false);

            builder.setFacebookLoginEnabled(true);
            builder.setFacebookLoginPermissions(Arrays.asList("public_profile", "user_friends"/*, "user_relationships", "user_birthday", "user_location"*/));

            // builder.setAppLogo(R.drawable.app_logo);

            startActivityForResult(builder.build(), 0);
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(1);

        Utils.makeMeRequest(); // retrieve user's Facebook ID
    }

    private void logRun() {
        try {

            File file = new File(Environment.getExternalStorageDirectory() + "/Places", String.valueOf(System.currentTimeMillis()));
            Log.d(TAG, "Logs directory: " + Environment.getExternalStorageDirectory() + "/Places");
            Runtime.getRuntime().exec("logcat -d -v time -f " + file.getAbsolutePath());
        }
        catch (IOException e){
            Log.w(TAG, "Something went wrong while starting the log: " + e.toString());
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if(id == R.id.action_logout)
        {
            logout();
        }
        else if(id == R.id.action_refresh)
        {
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Log the user out
        ParseUser.logOut();

        Utils.clearUserData();

        // Go to the login view
        startLoginActivity();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void refresh()
    {
        ((MosaicFragment)fragments[1]).updateFlags();
        ((MMapFragment)fragments[2]).updateMarkersOnMap();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        Log.d(TAG, "Page selected, is Location Service running? " + isMyServiceRunning(LocationService.class));
        Fragment sel = fragments[i];
        if(sel instanceof MMapFragment){
            Log.d(TAG, "Page selected. Updating markers...");
            ((MMapFragment)sel).updateMarkersOnMap();
        }
        else if(sel instanceof MosaicFragment){
            Log.d(TAG, "Page selected. Updating flags...");
            ((MosaicFragment)sel).updateFlags();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        MainActivity mainActivity;

        public SectionsPagerAdapter(FragmentManager fm, MainActivity mainActivity) {
            super(fm);
            this.mainActivity = mainActivity;
        }

        public int getCount() {
            return 3;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.share);
                case 1:
                    return getString(R.string.mosaic);
                case 2:
                    return getString(R.string.map);
            }
            return null;
        }

        public Fragment getItem(int i) {
            if (mainActivity.fragments[i] == null)
            {
                if(i == 0) mainActivity.fragments[i] = new ShareFragment();
                else if(i == 1) mainActivity.fragments[i] = new MosaicFragment();
                else mainActivity.fragments[i] = new MMapFragment();
            }
            return mainActivity.fragments[i];
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        isForeground = true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        isForeground = false;
    }

    public static boolean isForeground(){
        return isForeground;
    }
}
