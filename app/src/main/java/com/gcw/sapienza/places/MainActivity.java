package com.gcw.sapienza.places;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseAnalytics;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;



public class MainActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";

    private static boolean isForeground = false;

    private long startTime = -1;///used to track session timing (statistics)

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Fragment[] fragments = {new ShareFragment(), new MosaicFragment(), new MMapFragment()};
    private ViewPager mViewPager;
    private SwipeRefreshLayout srl;

    public SwipeRefreshLayout getSwipeRefreshLayout(){
        return this.srl;
    }
    public void setSwipeRefreshLayout(SwipeRefreshLayout srl){
        this.srl = srl;
    }



    public ShareFragment getShareFragment(){
        return (ShareFragment)this.fragments[0];
    }

    public MosaicFragment getMosaicFragment(){
        return (MosaicFragment)this.fragments[1];
    }

    public MMapFragment getMapFragment(){
        return (MMapFragment)this.fragments[2];
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlacesApplication.SINGLETON_INSTANCE = this;
        Utils.mainActivity = this;
        this.startTime = new Date().getTime();

        ParseAnalytics.trackAppOpenedInBackground(this.getIntent());
        setContentView(R.layout.activity_main);

        logRun();

        startLoginActivity();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(1);


        Utils.makeMeRequest(); // retrieve user's Facebook ID

        Resources res = getResources();
        Utils.categories = res.getStringArray(R.array.categories);
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
            ParseAnalytics.trackEventInBackground("open_settings");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if(id == R.id.action_logout)
        {
            ParseAnalytics.trackEventInBackground("logout");
            logout();
        }
        else if(id == R.id.action_refresh)
        {
            ParseAnalytics.trackEventInBackground("refresh");
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

    private void startLoginActivity()
    {
        if(ParseFacebookUtils.getSession() == null || ParseFacebookUtils.getSession().isClosed())
        {
            ParseLoginBuilder builder = new ParseLoginBuilder(MainActivity.this);

            builder.setParseLoginEnabled(false);

            builder.setFacebookLoginEnabled(true);
            builder.setFacebookLoginPermissions(Arrays.asList("public_profile", "user_friends"/*, "user_relationships", "user_birthday", "user_location"*/));

            // builder.setAppLogo(R.drawable.app_logo);

            startActivityForResult(builder.build(), 0);
        }
    }

    protected void refresh()
    {
        PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        Log.d(TAG, "Page selected, is Location Service running? " + isMyServiceRunning(LocationService.class));

        // I commented it out because I consider it an overkill -- Simone
        /*
        Fragment sel = fragments[i];
        if(sel instanceof MMapFragment){
            Log.d(TAG, "Page selected. Updating markers...");
            MMapFragment.updateMarkersOnMap();
        }
        else if(sel instanceof MosaicFragment){
            Log.d(TAG, "Page selected. Updating flags...");
            MosaicFragment.configureListViewWithFlags();
        }
        */

        Fragment sel = fragments[i];
        if(sel instanceof ShareFragment)
        {
            ((ShareFragment)sel).resetMedia();
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

    @Override
    public void onRefresh()
    {
        refresh();

        srl.setRefreshing(false);
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
        this.startTime = new Date().getTime();
        isForeground = true;
        Log.d(TAG,"resume");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        isForeground = false;

        /*
        long time_diff = ((new Date().getTime() - this.startTime)/1000)/60;
        Map<String, String> dimensions = new HashMap<String, String>();
        dimensions.put("time_minutes", ""+time_diff);
        ParseAnalytics.trackEventInBackground("session_time", dimensions);
        */
        Log.d(TAG,"pause");
    }

    public static boolean isForeground(){
        return isForeground;
    }

    public void takePic(View v)
    {
        this.getShareFragment().setPicTaken (true);

        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), Utils.PIC_CAPTURE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case Utils.PIC_CAPTURE_REQUEST_CODE:
                switch(resultCode)
                {
                    case RESULT_OK:
                        this.getShareFragment().setPicButtonAsPicTaken();

                        if(data.getData() == null){
                            this.getShareFragment().setPic ( (Bitmap)data.getExtras().get("data"));
                        }
                        else{
                            try{
                                this.getShareFragment().setPic (MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData()));
                            }
                            catch (IOException ioe){
                                ioe.printStackTrace();
                            }
                        }
                        break;

                }
        }
    }
}
