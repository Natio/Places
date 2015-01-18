package com.gcw.sapienza.places;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
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
import android.widget.Toast;

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
import java.util.List;


public class MainActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";

    private static boolean isForeground = false;


    private  Fragment[] fragments;// = {new ShareFragment(), new MosaicFragment(), new MMapFragment()};
    private SwipeRefreshLayout srl;

    private File imageFile;

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public MMapFragment getMapFragment(){
        return (MMapFragment)this.fragments[2];
    }



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Utils.mainActivity = this;

        ParseAnalytics.trackAppOpenedInBackground(this.getIntent());
        setContentView(R.layout.activity_main);

        logRun();

        startLoginActivity();

        if(savedInstanceState!=null){
            this.fragments = new Fragment[3];
            Log.d(TAG, savedInstanceState.toString());
            List<Fragment> fragments = this.getSupportFragmentManager().getFragments();
            for(Fragment f : fragments){
                if(f.getClass() == ShareFragment.class){
                    this.fragments[0] = f;
                }
                else if(f.getClass() == MMapFragment.class){
                    this.fragments[2] = f;
                }
                else if(f.getClass() == MosaicFragment.class){
                    this.fragments[1] = f;
                }
            }
        }
        else{
            this.fragments = new Fragment[]{new ShareFragment(), new MosaicFragment(), new MMapFragment()};
        }




        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(1);

        if(ParseFacebookUtils.getSession() != null && ParseFacebookUtils.getSession().isOpened()){
            this.startDownloadingFacebookInfo();
        }

        if(savedInstanceState != null && this.getShareFragment().isAdded())
        {
            this.getShareFragment().setPicture(savedInstanceState.getString("pic"));
            this.getShareFragment().setAudio(savedInstanceState.getString("audio"));
            this.getShareFragment().setVideo(savedInstanceState.getString("video"));

        }




        Resources res = getResources();
        Utils.categories = res.getStringArray(R.array.categories);
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

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString("audio", this.getShareFragment().getAudioPath());
        outState.putString("pic", this.getShareFragment().getPicPath());
        outState.putString("video", this.getShareFragment().getVideoPath());
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
        Location currentLocation = PlacesApplication.getInstance().getLocation();
        if(currentLocation != null){
            PlacesApplication.getInstance().getLocationService().queryParsewithLocation(currentLocation);
        }
        else
            Toast.makeText(this, "No location data available\n" +
                    "Are Location Services enabled?", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i)
    {
        Log.d(TAG, "Page selected: " + i);

    }

    @SuppressWarnings("unused")
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
        private final MainActivity mainActivity;

        public SectionsPagerAdapter(FragmentManager fm, MainActivity mainActivity)
        {
            super(fm);
            this.mainActivity = mainActivity;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
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
        @Override
        public Fragment getItem(int i)
        {
            /*if (mainActivity.fragments[i] == null)
            {
                if(i == 0) mainActivity.fragments[i] = new ShareFragment();
                else if(i == 1) mainActivity.fragments[i] = new MosaicFragment();
                else mainActivity.fragments[i] = new MMapFragment();
            }*/
            return mainActivity.fragments[i];
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                &&!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            promptForLocationServices();
        }else {
            PlacesApplication.getInstance().startLocationService();
        }

        isForeground = true;
    }

    private void promptForLocationServices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Services disabled");
        builder.setCancelable(false);
        builder.setMessage("Places requires Location Services to be turned on in order to work properly.\n" +
                "Edit Location Settings?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(new Intent
                        (android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), Utils.GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
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

    }

    public static boolean isForeground(){
        return isForeground;
    }

    public void takePic(View v)
    {
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePicture.resolveActivity(this.getPackageManager()) != null){
            this.imageFile = null;
            try{
                this.imageFile = Utils.createImageFile(ShareFragment.PICTURE_FORMAT);
            }
            catch (IOException e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            if(this.imageFile != null){
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(this.imageFile));
                this.startActivityForResult(takePicture, Utils.PIC_CAPTURE_REQUEST_CODE);
            }
        }
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
                        if(this.imageFile == null || !this.imageFile.canRead()){
                            Toast.makeText(getApplicationContext(), "Error encountered while taking picture", Toast.LENGTH_LONG).show();
                            Log.v(TAG, "Error encountered while taking picture");
                            this.imageFile = null;
                            break;
                        }

                        /*Log.d(TAG, this.imageFile.getAbsolutePath());
                        Bitmap bitmap = BitmapFactory.decodeFile(this.imageFile.getAbsolutePath());
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                        this.getShareFragment().setPicture(stream.toByteArray());*/
                        this.getShareFragment().setPicture(this.imageFile.getAbsolutePath());
                        this.imageFile = null;

                       break;
                    case RESULT_CANCELED:
                        Log.v(TAG, "Camera Intent canceled");
                        break;
                }
            case Utils.GPS_ENABLE_REQUEST_CODE:
                PlacesApplication.getInstance().startLocationService();
                break;
            case Utils.LOGIN_REQUEST_CODE:

                if(resultCode == RESULT_OK){
                    this.startDownloadingFacebookInfo();
                }

                break;
            case Utils.VID_SHOOT_REQUEST_CODE:
                switch (resultCode)
                {
                    case RESULT_OK:
                        Uri videoUri = data.getData();

                        //File file = new File(getRealPathFromURI(this, videoUri));
                        //FileInputStream inStream = new FileInputStream(file);
                        String videoPath = this.getRealPathFromURI(this, videoUri);
                        this.getShareFragment().setVideo(videoPath);

                        break;
                    case RESULT_CANCELED:
                        Log.v(TAG, "Video Intent canceled");
                }
        }
    }

    public void shootVid(View v)
    {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (videoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(videoIntent, Utils.VID_SHOOT_REQUEST_CODE);
        }

    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Video.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


}
