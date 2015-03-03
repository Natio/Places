package com.gcw.sapienza.places.activities;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.fragments.MainFragment;
import com.gcw.sapienza.places.fragments.MyFlagsFragment;
import com.gcw.sapienza.places.fragments.ProfileFragment;
import com.gcw.sapienza.places.fragments.SettingsFragment;
import com.gcw.sapienza.places.utils.GPlusUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;


public class MainActivity extends ActionBarActivity implements Preference.OnPreferenceChangeListener {


    public static String TAG = MainActivity.class.getName();
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private static final String[] section_titles = {"Home", "My Profile", "My Flags", "Settings", "Logout"};
    private CharSequence current_title;

    // private int currentDrawerListItemIndex = -1;

    private LinearLayout homeHolder;
    private FrameLayout fragHolder;

    public Menu mMenu;

    private static final int SHARE_ACTIVITY_REQUEST_CODE = 95;

    private static final int FLAGS_LIST_POSITION = 0;
    private static final int MY_PROFILE_POSITION = 1;
    private static final int MY_FLAGS_POSITION = 2;
    private static final int SETTINGS_POSITION = 3;
    private static final int LOGOUT_POSITION = 4;

    private static final String FRAG_TAG = "FRAG_TAG";

    private Toast radiusToast;

    private static boolean isForeground = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PlacesLoginUtils.getInstance().isSessionValid(this))
            PlacesLoginUtils.getInstance().downloadUserInfo(this);
        else PlacesLoginUtils.startLoginActivity(this);

        setContentView(R.layout.activity_main_drawer_layout);
        this.current_title = this.getTitle();
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.drawerList = (ListView) findViewById(R.id.left_drawer);

        this.homeHolder = (LinearLayout) findViewById(R.id.home_container);
        this.fragHolder = (FrameLayout) findViewById(R.id.frag_container);

        // Set the adapter for the list view
        this.drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, section_titles));

        this.drawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.drawable.ic_drawer, R.drawable.ic_drawer) {
            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                MainActivity.this.getSupportActionBar().setTitle(MainActivity.this.current_title);
                unHighlightSelection();
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // MainActivity.this.getSupportActionBar().setTitle("To_find_a_title");
            }
        };

        this.drawerLayout.setDrawerListener(this.drawerToggle);
        this.drawerList.setOnItemClickListener(new DrawerItemClickListener());

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

//        this.selectItem(0);

        this.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));

        this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, new MainFragment()).commit();
    }

    @Deprecated
    private int getIconForCategory(String category) {
        String[] category_array = this.getResources().getStringArray(R.array.categories);

        if (category == null || category.equals(category_array[0])) return R.drawable.flag_red;
        else if (category.equals(category_array[1])) return R.drawable.flag_green;
        else if (category.equals(category_array[2])) return R.drawable.flag_yellow;
        else if (category.equals(category_array[3])) return R.drawable.flag_blue;
        else return R.drawable.flag_purple; // 'Food' category
    }

    @Deprecated
    private float getCategoryColor(String category) {
        String[] category_array = this.getResources().getStringArray(R.array.categories);

        if (category == null || category.equals(category_array[0]))
            return BitmapDescriptorFactory.HUE_RED;
        else if (category.equals(category_array[1])) return BitmapDescriptorFactory.HUE_AZURE;
        else if (category.equals(category_array[2])) return BitmapDescriptorFactory.HUE_ORANGE;
        else if (category.equals(category_array[3])) return BitmapDescriptorFactory.HUE_BLUE;
        else return BitmapDescriptorFactory.HUE_MAGENTA; // 'Food' category
    }

    public void refresh() {
        Location currentLocation = PlacesApplication.getInstance().getLocation();
        if (currentLocation != null) {
            PlacesApplication.getInstance().getLocationService().queryParsewithLocation(currentLocation);
        } else
            Toast.makeText(this, "No location data available\n" +
                    "Are Location Services enabled?", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        this.drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.action_add_flag) {
            Intent shareIntent = new Intent(this, ShareActivity.class);
            startActivityForResult(shareIntent, MainActivity.SHARE_ACTIVITY_REQUEST_CODE);
            // item.setVisible(false);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            promptForLocationServices();
        } else {
            PlacesApplication.getInstance().startLocationService();
        }

        isForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        isForeground = false;

    }

    public static boolean isForeground() {
        return isForeground;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_drawer, menu);

        mMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = this.drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.action_add_flag).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    //    }
//        }
//            switchToListMapFrags();
//        {
//        if(homeHolder.getVisibility() == View.INVISIBLE)
//    {
//    public void onBackPressed()
//    }
//        this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, new MainFragment()).addToBackStack(null).commit();
//
//        switchToSupportFrag();
//        mapFragment.getMapAsync(this);
//        this.getSupportFragmentManager().beginTransaction().replace(R.id.map_holder, mapFragment).commit();
//        SupportMapFragment mapFragment = new SupportMapFragment();
//
//        this.getSupportFragmentManager().beginTransaction().replace(R.id.swipe_refresh, fragment).commit();
//        Fragment fragment = new FlagsListFragment();
//
//        fragHolder.setVisibility(View.INVISIBLE);
//        homeHolder.setVisibility(View.VISIBLE);
//
//        getRidOfUnusedFrag();
//    {

    private void myProfile() {
        switchToOtherFrag(ProfileFragment.newInstance(PlacesLoginUtils.getInstance().getCurrentUserId()));
    }

    private void logout() {
        // Log the user out
        ParseUser.logOut();

        // TODO It needs to be coupled with ParseUser.logOut()
        if (PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.GPLUS &&
                GPlusUtils.getInstance().getGoogleApiClient() != null)
            GPlusUtils.getInstance().getGoogleApiClient().disconnect();

        PlacesLoginUtils.getInstance().clearUserData();

        // Go to the login view
        PlacesLoginUtils.startLoginActivity(this);
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        // TODO I have temporarily removed the block of code below for my implementation to work
        /*
        if(this.currentDrawerListItemIndex == position)
        {
            this.drawerLayout.closeDrawers();

            return;
        }
        */
        switch (position) {

            case FLAGS_LIST_POSITION:
//                if(homeHolder.getVisibility() == View.INVISIBLE)
                switchToOtherFrag(new MainFragment());
                break;

            case MY_PROFILE_POSITION:
                myProfile();
                break;

            case MY_FLAGS_POSITION:
                switchToOtherFrag(new MyFlagsFragment());
                break;

            case SETTINGS_POSITION:
                switchToSettingsFrag();
                break;

            case LOGOUT_POSITION:
                logout();
                break;

            default:
                Log.w(TAG, "Selected item not found in drawer");

        }
        this.drawerLayout.closeDrawers();
    }


    private void unHighlightSelection() {
        int toClear = this.drawerList.getCheckedItemPosition();

        if (toClear >= 0) drawerList.setItemChecked(toClear, false);
    }

    @Override
    public void setTitle(CharSequence title) {
        this.current_title = title;
        this.getSupportActionBar().setTitle(this.current_title);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            MainActivity.this.selectItem(position);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Utils.LOGIN_REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    //this.startDownloadingFacebookInfo();

                    // TODO At this time, if G+ login is being used, this code block won't be executed
                    Log.d(TAG, "Login was successful");

                    if (ParseFacebookUtils.getSession() == null)
                        PlacesLoginUtils.loginType = PlacesLoginUtils.LoginType.GPLUS;
                    else PlacesLoginUtils.loginType = PlacesLoginUtils.LoginType.FACEBOOK;

                    PlacesLoginUtils.getInstance().downloadUserInfo(this);
                }
                break;
            case SHARE_ACTIVITY_REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, data.getExtras().getString("result"), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void getRidOfUnusedFrag() {
        android.app.Fragment frag1 = this.getFragmentManager().findFragmentByTag(FRAG_TAG);
        Fragment frag2 = this.getSupportFragmentManager().findFragmentByTag(FRAG_TAG);

        // TODO We need to check if we need both lines since we got both "fragments" and "support fragments"
        if (frag1 != null) this.getFragmentManager().beginTransaction().remove(frag1).commit();
        else if (frag2 != null)
            this.getSupportFragmentManager().beginTransaction().remove(frag2).commit();
    }

    private void switchToSupportFrag() {
        fragHolder.setVisibility(View.INVISIBLE);
        homeHolder.setVisibility(View.VISIBLE);
    }

    private void switchToNonSupportFrag() {
        homeHolder.setVisibility(View.INVISIBLE);
        fragHolder.setVisibility(View.VISIBLE);
    }

//    private void switchToListMapFrags()

//        this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, new Fragment()).commit();

    private void switchToSettingsFrag() {
        Log.d(TAG, "Switching to SettingsFragment");
//        getRidOfUnusedFrag();
        switchToNonSupportFrag();

        this.getFragmentManager().beginTransaction().replace(R.id.frag_container, new SettingsFragment()).commit();
    }

    public void switchToOtherFrag(Fragment frag) {
        Log.d(TAG, "Switching to other fragment: " + frag.getClass());
//        getRidOfUnusedFrag();
//        switchToFragOtherThanHome();
        Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.home_container);
        if (!isNonSupportFragmentVisible() && f != null && f.getClass() == frag.getClass()) {
            Log.w(TAG, "Switching to the same fragment: " + f.getClass());
            return;
        }

        switchToSupportFrag();

        this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, frag).addToBackStack(null).commit();
    }

//    @Override

    private boolean isNonSupportFragmentVisible() {
        return fragHolder.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onBackPressed() {
        Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.home_container);
        if (!isNonSupportFragmentVisible() && f != null && f.getClass() == MainFragment.class) {
            Log.d(TAG, "Pressed back button on MainFragment: finishing...");
            this.getSupportFragmentManager().popBackStack(null, android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            finish();
            return;
        }
        FragmentManager fm = getFragmentManager();
//        if (fm.getBackStackEntryCount() > 0) {
//            Log.i("MainActivity", "popping backstack: " +
//                    fm.getBackStackEntryCount() + ", while support: " +
//                    getSupportFragmentManager().getBackStackEntryCount());
//            fm.popBackStack();
//        }
//        else
        if (isNonSupportFragmentVisible()) {
            switchToSupportFrag();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            Log.i("MainActivity", "popping support: " +
                    getSupportFragmentManager().getBackStackEntryCount() + ", while backstack: " +
                    fm.getBackStackEntryCount());
            getSupportFragmentManager().popBackStack();
        } else {
            Log.i("MainActivity", "nothing on backstack, calling super");
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        if (preference.getKey().equals("meFilter") ||
                preference.getKey().equals("flFilter") ||
                preference.getKey().equals("strangersFilter") ||
                preference.getKey().equals("timeFilter") ||
                preference.getKey().equals("thoughtsCheck") ||
                preference.getKey().equals("funCheck") ||
                preference.getKey().equals("musicCheck") ||
                preference.getKey().equals("landscapeCheck") ||
                preference.getKey().equals("foodCheck") ||
                preference.getKey().equals("noneCheck")) {
            Log.d(TAG, "Called onPreferenceChange for: " + preference.getKey());
            editor.putBoolean(preference.getKey(), (boolean) newValue);
            editor.commit();
        }
        /*
        else if(preference.getKey().equals("seekBar"))
        {
            preference.setDefaultValue(newValue);
            int value = (int)newValue + 1;
            Utils.MAP_RADIUS = value / 10f;
            showToast("Radius set to " + value * 100 + " meters.");
            Log.d(TAG, "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }
        */
        else if (preference.getKey().equals("maxFetch")) {
            preference.setDefaultValue(newValue);
            int value = Utils.stepValues[(int) newValue];
            Utils.MAX_FLAGS = value;
            showToast("Max number of visible flags: " + value);
        }

        Location currentLocation = PlacesApplication.getInstance().getLocation();
        PlacesApplication.getInstance().getLocationService().queryParsewithLocation(currentLocation);

        return true;
    }

    private void showToast(String text) {
        if (radiusToast != null)
            radiusToast.cancel();
        radiusToast = Toast.makeText(getBaseContext(), text,
                Toast.LENGTH_SHORT);
        radiusToast.show();
    }

}





