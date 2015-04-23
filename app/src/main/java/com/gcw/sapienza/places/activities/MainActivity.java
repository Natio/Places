/*
 * Copyright 2015-present Places®.
 */

package com.gcw.sapienza.places.activities;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import com.gcw.sapienza.places.adapters.InboxAdapter;
import com.gcw.sapienza.places.fragments.BagFragment;
import com.gcw.sapienza.places.fragments.CategoriesFragment;
import com.gcw.sapienza.places.fragments.FlagFragment;
import com.gcw.sapienza.places.fragments.InboxFragment;
import com.gcw.sapienza.places.fragments.MainFragment;
import com.gcw.sapienza.places.fragments.MyFlagsFragment;
import com.gcw.sapienza.places.fragments.PlacesMapListFragment;
import com.gcw.sapienza.places.fragments.ProfileFragment;
import com.gcw.sapienza.places.fragments.SettingsFragment;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.services.ILocationServiceListener;
import com.gcw.sapienza.places.utils.GPlusUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.parse.GetCallback;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.List;

/*
 * The following activity is the homepage of the app where the list of flags is contained
 */

public class MainActivity extends ActionBarActivity implements Preference.OnPreferenceChangeListener, ResultCallback<People.LoadPeopleResult>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ILocationServiceListener{

    public static final String TAG = MainActivity.class.getName();
    private static final String[] section_titles = {"Home", "Profile", "Inbox", "My Flags", "Bag", "Settings", "Logout"};
    private static final String FRAG_TAG = "FRAG_TAG";

    private static final int SHARE_ACTIVITY_REQUEST_CODE = 95;
    private static final int FLAGS_LIST_POSITION = 0;
    private static final int MY_PROFILE_POSITION = 1;
    private static final int INBOX_POSITION = 2;
    private static final int MY_FLAGS_POSITION = 3;
    private static final int BAG_POSITION = 4;
    private static final int SETTINGS_POSITION = 5;
    private static final int LOGOUT_POSITION = 6;

    private static boolean isForeground = false;
    private boolean loggedIn;

    public Menu mMenu;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence current_title;
    private LinearLayout homeHolder;
    private FrameLayout fragHolder;
    private Toast radiusToast;

    private boolean preferencesChangedFlag = false;

    public static final String PREFERENCES_CHANGED_NOTIFICATION = "Preferences Changed";

    public static boolean isForeground() {
        return isForeground;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlacesLoginUtils.getInstance().checkForSessionValidityAndStartDownloadingInfo(this);

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
        this.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));

        // If app is opened by clicking of comment notification, go straight to the flag for which you've been notified
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "Bundle is not null.");
            handleIntent(intent);
        } else {
            Log.d(TAG, "Bundle is null. Triggering default Activity behavior");
            PlacesApplication.getInstance().startLocationService();
            //new interface
            this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, new MainFragment()).commit();
            //this.getSupportFragmentManager().beginTransaction().replace(R.id.sliding_layout, new MainFragment()).commit();
        }
        // setIntent(null);
        registerReceivers();
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(this.receiver, new IntentFilter(MainActivity.PREFERENCES_CHANGED_NOTIFICATION));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case MainActivity.PREFERENCES_CHANGED_NOTIFICATION:
                    //set the preferences changed
                    preferencesChangedFlag = true;
                    //no automatic refresh of application data on preference change
                    break;
                default:
                    Log.w(MainActivity.class.getName(), intent.getAction() + ": cannot identify the received notification");
            }
        }
    };

    private void handleIntent(Intent intent) {
        String bundleContentType = intent.getStringExtra("type");
        if (bundleContentType == null) {
            Log.d(TAG, "String 'type' in bundle is null. Triggering default Activity behavior.");

            PlacesApplication.getInstance().startLocationService();
            //new interface
            this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, new MainFragment()).commit();
            //this.getSupportFragmentManager().beginTransaction().replace(R.id.sliding_layout, new MainFragment()).commit();
        } else {
            switch (bundleContentType){
                case PlacesUtils.RECEIVED_NOTIF_COMMENT_TYPE:
                    Log.d(TAG, PlacesUtils.RECEIVED_NOTIF_COMMENT_TYPE);
                    String flagId = intent.getStringExtra(PlacesUtils.FLAG_ID);
                    openFlagFromId(flagId);
                    break;

                case PlacesUtils.RECEIVED_MULTI_NOTIF_COMMENT_TYPE:
                    Log.d(TAG, PlacesUtils.RECEIVED_MULTI_NOTIF_COMMENT_TYPE);
                    switchToOtherFrag(new InboxFragment());
                    break;

                default:
                    Log.w(TAG, "Unrecognized bundleContentType");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
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


    public void refresh(int updateCode) {
        Log.d(TAG, "Refreshing application with code: " + updateCode);
        PlacesApplication.getInstance().updatePlacesData(this, updateCode);
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
        } //attempt to add filters in homepage
        else if (item.getItemId() == R.id.filters) {
            switchToNonSupportFrag(new CategoriesFragment());
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean areLocationServicesEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!areLocationServicesEnabled()) {
            promptForLocationServices();
        } else if(loggedIn){
            if(!PlacesApplication.getInstance().isLocationServiceRunning()){
                PlacesApplication.getInstance().startLocationService();
            }
        }

        /*
        Intent intent = getIntent();

        Log.d(TAG, "Is intent null? " + (intent==null));

        String bundleContentType = intent.getStringExtra("type");
        if(bundleContentType != null && bundleContentType.equals(Utils.RECEIVED_NOTIF_COMMENT_TYPE))
        {
            Log.d(TAG, "String 'type' in bundle is not null");

            String flagId = intent.getStringExtra(Utils.FLAG_ID);
            openFlagFromId(flagId);
        }
        else Log.d(TAG, "String 'type' in bundle is null");
        */

        isForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isForeground = false;
    }

    private void promptForLocationServices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Services disabled");
        builder.setCancelable(true);
        builder.setMessage("Places requires Location Services to be turned on to provide the best experience.\n" +
                "Edit Location Settings?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(new Intent
                        (android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), PlacesUtils.GPS_ENABLE_REQUEST_CODE);
            }
        });

        //Allow users to use Places with no Location Services turned on
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                /**
                 * if 'NO' is clicked, back to MainActivity focus
                 */
                //finish();
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

    private void myProfile() {
        switchToOtherFrag(ProfileFragment.newInstance((PlacesUser)PlacesUser.getCurrentUser()));
    }

    private void logout() {
        // Log the user out
        ParseUser.logOut();

        // TODO It needs to be coupled with ParseUser.logOut()
        if (PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.GPLUS &&
                GPlusUtils.getInstance().getGoogleApiClient() != null)
            GPlusUtils.getInstance().getGoogleApiClient().disconnect();

        PlacesLoginUtils.getInstance().clearUserData();

        this.loggedIn = false;

        // Go to the login view
        PlacesLoginUtils.startLoginActivity(this, true);
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

            case INBOX_POSITION:
                switchToOtherFrag(new InboxFragment());
                break;

            case MY_FLAGS_POSITION:
                switchToOtherFrag(new MyFlagsFragment());
                break;

            case SETTINGS_POSITION:
                switchToNonSupportFrag(new SettingsFragment());
                break;

            case BAG_POSITION:
                switchToOtherFrag(new BagFragment());
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

    //for some reason, inbox clear button onClick
    // callback listener setting doesn't work via Java code
    // it has been set in inbox_layout.xml
    public void onInboxClearButtonClick(View v){
        Log.d(TAG, "Clearing Inbox...");
        try {
            PlacesStorage.clearInbox(this);
            List<List<String>> inbox = PlacesStorage.fetchInbox(this);
            LinearLayout llInbox = (LinearLayout)v.getParent();
            ListView inboxListView = (ListView)llInbox.findViewById(R.id.inbox_list_view);
            inboxListView.setAdapter(new InboxAdapter(this, R.layout.inbox_message_layout, inbox));
        } catch (IOException e) {
            Log.e(TAG, "Error", e);
            PlacesUtils.showToast(this, "Something went wrong while clearing Inbox data", Toast.LENGTH_SHORT);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Error", e);
            PlacesUtils.showToast(this, "Something went wrong while loading Inbox data", Toast.LENGTH_SHORT);
        }
    }

    public boolean arePreferencesChanged(){
        return this.preferencesChangedFlag;
    }

    public void resetPreferencesFlag(){
        this.preferencesChangedFlag = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PlacesUtils.LOGIN_REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    //this.startDownloadingFacebookInfo();

                    this.loggedIn = true;

                    // TODO At this time, if G+ login is being used, this code block won't be executed
                    Log.d(TAG, "Login was successful");

                    if (ParseFacebookUtils.getSession() == null)
                        PlacesLoginUtils.loginType = PlacesLoginUtils.LoginType.GPLUS;
                    else PlacesLoginUtils.loginType = PlacesLoginUtils.LoginType.FACEBOOK;

                    switchToOtherFrag(new MainFragment());

                    PlacesLoginUtils.downloadUserInfo(this);
                    ParseInstallation.getCurrentInstallation().put("owner", ParseUser.getCurrentUser());
                    ParseInstallation.getCurrentInstallation().saveInBackground();
                }
                break;
            case SHARE_ACTIVITY_REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, data.getExtras().getString("result"), Toast.LENGTH_LONG).show();
                    this.refresh(PlacesUtils.NEARBY_FLAGS_CODE);
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

    private void showSupportFrag() {
        fragHolder.setVisibility(View.INVISIBLE);
        homeHolder.setVisibility(View.VISIBLE);
        //change for new layout
        //mLayout.setVisibility(View.VISIBLE);
    }

    private void showNonSupportFrag() {
        fragHolder.setVisibility(View.VISIBLE);
        homeHolder.setVisibility(View.INVISIBLE);
        //change for new layout
        //mLayout.setVisibility(View.INVISIBLE);
    }

    private void switchToNonSupportFrag(android.app.Fragment frag) {
        Log.d(TAG, "Switching to FilterListFragment");
        android.app.Fragment f = this.getFragmentManager().findFragmentById(R.id.frag_container);
        if (isNonSupportFragmentVisible() && f != null && f.getClass() == frag.getClass()) {
            Log.w(TAG, "Hiding non-support fragment: " + f.getClass());
            showSupportFrag();
            return;
        }

        showNonSupportFrag();

        this.getFragmentManager().beginTransaction().replace(R.id.frag_container, frag).commit();
    }

    public void switchToOtherFrag(Fragment frag) {
        Log.d(TAG, "Switching to other fragment: " + frag.getClass());
        //new interface
        Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.home_container);
        //Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.sliding_layout);

        showSupportFrag();
        //new interface
        this.getSupportFragmentManager().beginTransaction().replace(R.id.home_container, frag).addToBackStack(null).commit();
        //this.getSupportFragmentManager().beginTransaction().replace(R.id.sliding_layout, frag).addToBackStack(null).commit();
    }

    private boolean isNonSupportFragmentVisible() {
        return fragHolder.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onBackPressed() {
        //new interface
        Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.home_container);

        if (!isNonSupportFragmentVisible() && f != null && f.getClass() == MainFragment.class) {

            int first_backstack_id = this.getSupportFragmentManager().getBackStackEntryAt(0).getId();
            String first_backstack_name = this.getSupportFragmentManager().getBackStackEntryAt(0).getName();

            Log.d(TAG, "Pressed back button on MainFragment: finishing...");
            this.getSupportFragmentManager().popBackStack(null, android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

            Fragment onBack = this.getSupportFragmentManager().findFragmentById(first_backstack_id);
            Log.d(TAG, "First fragment on backstack to be popped and refreshed: " + first_backstack_name);

            PlacesMapListFragment toBeRefreshedFragment = (PlacesMapListFragment)onBack;
            toBeRefreshedFragment.refresh();

            finish();

        } else if (isNonSupportFragmentVisible()) {
            //if here, settings are visible
            if(arePreferencesChanged()) {
                PlacesMapListFragment toBeRefreshedFragment = (PlacesMapListFragment) f;
                toBeRefreshedFragment.refresh();
                resetPreferencesFlag();
            }
            showSupportFrag();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            FragmentManager fm = getFragmentManager();

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

        Log.d(TAG, "Preferences changed");

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
                preference.getKey().equals("noneCheck") ||
                preference.getKey().equals("notificationsCheck")) {
            Log.d(TAG, "Called onPreferenceChange for: " + preference.getKey());
            editor.putBoolean(preference.getKey(), (boolean) newValue);
            editor.commit();
        }
        else if (preference.getKey().equals("maxFetch")) {
            preference.setDefaultValue(newValue);
            int value = PlacesUtils.STEP_VALUES[(int) newValue];
            showToast("Max number of visible flags: " + value);
        }

        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(MainActivity.PREFERENCES_CHANGED_NOTIFICATION));

        return true;
    }

    private void showToast(String text) {
        if (radiusToast != null)
            radiusToast.cancel();
        radiusToast = Toast.makeText(getBaseContext(), text,
                Toast.LENGTH_SHORT);
        radiusToast.show();
    }

    @Override
    public void onServiceConnected(int updateCode) {
        Log.d(TAG, "LocationService is connected! Callback is called");
        switch (updateCode){
            case PlacesUtils.NEARBY_FLAGS_CODE:
                Log.d(TAG, "Service Connected. Updating Nearby Flags");
                PlacesApplication.getInstance().updatePlacesData(this, PlacesUtils.NEARBY_FLAGS_CODE);
                break;
            case PlacesUtils.MY_FLAGS_CODE:
                Log.d(TAG, "Service Connected. Updating My Flags");
                PlacesApplication.getInstance().updatePlacesData(this, PlacesUtils.MY_FLAGS_CODE);
                break;
            case PlacesUtils.BAG_FLAGS_CODE:
                Log.d(TAG, "Service Connected. Updating Bag Flags");
                PlacesApplication.getInstance().updatePlacesData(this, PlacesUtils.BAG_FLAGS_CODE);
                break;
            case PlacesUtils.DEFAULT_FLAGS_CODE:
                Log.d(TAG, "Triggering default behavior: Updating whole Flag data");
                PlacesApplication.getInstance().updatePlacesData(this);
                break;
            default:
                Log.d(TAG, "Triggering default behavior: Updating whole Flag data");
                PlacesApplication.getInstance().updatePlacesData(this);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            MainActivity.this.selectItem(position);
        }
    }

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {
        Log.d(TAG, "Result from People request:" + loadPeopleResult.getStatus());

        if (loadPeopleResult.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
            PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
            try {
                int count = personBuffer.getCount();
                for (int i = 0; i < count; i++) {
                    Person person = personBuffer.get(i);
                    Log.d(TAG, "Display name: " + person.getDisplayName());
                    PlacesLoginUtils.getInstance().addEntryToUserIdMap(person.getId(), person.getDisplayName());
                    PlacesLoginUtils.getInstance().addFriend(person.getDisplayName());
                }
            } finally {
                personBuffer.close();
            }
        } else {
            Log.e(TAG, "Error requesting people data: " + loadPeopleResult.getStatus());
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO handle this!
        Log.d(TAG, "Login failed");
        Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.

        GPlusUtils.getGPlusUsername(this);
        GPlusUtils.getGPlusFriends(this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        GPlusUtils.getInstance().getGoogleApiClient().connect();
    }

    public void openFlagFromId(String flagId)
    {
        ParseQuery<Flag> q = ParseQuery.getQuery("Posts");
        q.whereEqualTo("objectId", flagId);
        q.getFirstInBackground(new GetCallback<Flag>() {
            @Override
            public void done(Flag flag, com.parse.ParseException e)
            {
                if(e != null){
                    Log.d(TAG, e.getMessage());
                }
                else{
                    openFlag(flag);
                }
            }
        });
    }

    private void openFlag(Flag f)
    {
        FlagFragment frag = new FlagFragment();
        frag.setScrollToLastComment(true);
        frag.setFlag(f);

        switchToOtherFrag(frag);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this.receiver);
    }

    @Override
    public void onDestroy(){
        unregisterReceivers();
        super.onDestroy();
    }
}