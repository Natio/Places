package com.gcw.sapienza.places;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.LocationManager;
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
import android.view.ViewGroup;
import android.widget.Toast;

import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
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

    private Fragment[] fragments = {new ShareFragment(), new MosaicFragment(), new MMapFragment()};
    private SectionsPagerAdapter mSectionsPagerAdapter;
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

    public MosaicFragment getMosaicFragment(){ return (MosaicFragment)this.fragments[1]; }

    public MMapFragment getMapFragment(){
        return (MMapFragment)this.fragments[2];
    }



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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

        if(ParseFacebookUtils.getSession() != null && ParseFacebookUtils.getSession().isOpened()){
            this.startDownloadingFacebookInfo();
        }


        Resources res = getResources();
        Utils.categories = res.getStringArray(R.array.categories);

        for(int i = 0; i < fragments.length; i++) fragments[i].setRetainInstance(true);
    }

    private void startDownloadingFacebookInfo(){
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.show();
        FacebookUtils.getInstance().makeMeRequest(new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                if(e != null){
                    Log.d(TAG, e.getMessage());
                    progress.setMessage(e.getMessage());
                }
                else{
                    progress.dismiss();
                    Log.d(TAG, result);
                }
            }
        }); // retrieve user's Facebook ID
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
        /*
        else if(id == R.id.action_refresh)
        {
            ParseAnalytics.trackEventInBackground("refresh");
            refresh();
        }
        */

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Log the user out
        ParseUser.logOut();

        FacebookUtils.getInstance().clearUserData();

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

            startActivityForResult(builder.build(), Utils.LOGIN_REQUEST_CODE);
        }
    }

    protected void refresh()
    {
        if(PlacesApplication.getLocation() != null)
            PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());
        else
            Toast.makeText(this, "No location data available\n" +
                    "Are Location Services enabled?", Toast.LENGTH_LONG).show();
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

        Log.d(TAG, "Page selected: " + i);

        if(i == 0)
        {
            ((ShareFragment)fragments[i]).resetMedia();
            ((ShareFragment)fragments[i]).onVisiblePage();
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

        public SectionsPagerAdapter(FragmentManager fm, MainActivity mainActivity)
        {
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

        /*
        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            Log.d(TAG, "Item at position " + position + " was destroyed!");
        }
        */

        public Fragment getItem(int i)
        {
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

        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                &&!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            promptForLocationServices();
        }else {
            PlacesApplication.placesApplication.startLocationService();
        }

        isForeground = true;
        Log.d(TAG,"resume");
    }

    private void promptForLocationServices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Services disabled");
        builder.setCancelable(false);
        builder.setMessage("Places requires Location Services to be turned on in order to work properly.\n" +
                "Edit Location Settings?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(new Intent
                        (android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), Utils.GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
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
                        Log.v(TAG, "Camera Intent OK");
                        // this.getShareFragment().setPicButtonAsPicTaken();

                        if(data.getData() == null)
                        {
                            Log.v(TAG, "getData() returns null in OnActivityResult");
                            Bitmap image = (Bitmap)data.getExtras().get("data");

                            if(image == null)
                            {
                                Toast.makeText(getApplicationContext(), "Error encountered while taking picture", Toast.LENGTH_LONG).show();
                                Log.v(TAG, "Error encountered while taking picture");
                            }

                            this.getShareFragment().pic = (Bitmap)data.getExtras().get("data");
                            // Utils.pic = (Bitmap)data.getExtras().get("data");
                        }
                        else
                        {
                            Log.v(TAG, "getData() is not null in OnActivityResult");
                            try
                            {
                                this.getShareFragment().pic = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                            }
                            catch (IOException ioe){
                                ioe.printStackTrace();
                            }
                        }
                        break;
                    case RESULT_CANCELED:
                        Log.v(TAG, "Camera Intent canceled");
                        break;
                }
            case Utils.GPS_ENABLE_REQUEST_CODE:
                PlacesApplication.placesApplication.startLocationService();
                break;
            case Utils.LOGIN_REQUEST_CODE:

                if(resultCode == RESULT_OK){
                    this.startDownloadingFacebookInfo();
                }

                break;
        }
    }
}
