package com.gcw.sapienza.places.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gcw.sapienza.places.NoFlagsFragment;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.SettingsFragment;
import com.gcw.sapienza.places.ShareActivity;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.model.people.Person;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.List;


public class MainActivity2 extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener,
                                                                OnMapReadyCallback,
                                                                Preference.OnPreferenceChangeListener {

    public static String TAG = MainActivity2.class.getName();
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private static final String [] section_titles = {"Home", "Settings", "Logout"};
    private CharSequence current_title;
    private MSwipeRefreshLayout srl;
    // private int currentDrawerListItemIndex = -1;

    private LinearLayout homeHolder;
    private FrameLayout fragHolder;

    public Menu mMenu;

    private static final int SHARE_ACTIVITY_REQUEST_CODE = 95;

    private static final int FLAGS_LIST_POSITION = 0;
    private static final int SETTINGS_POSITION = 1;
    private static final int LOGOUT_POSITION = 2;

    private static final int MAP_BOUNDS = 70;
    private static final float ZOOM_LVL = 16f;

    private static final String FRAG_TAG = "FRAG_TAG";

    private GoogleMap gMap;

    private Toast radiusToast;

    private BroadcastReceiver receiver;

    private static boolean isForeground = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(FacebookUtils.isFacebookSessionOpened())
        {
            FacebookUtils.downloadFacebookInfo(this);
        }
        else
        {
            FacebookUtils.startLoginActivity(this);
        }

        setContentView(R.layout.activity_main_drawer_layout);
        this.current_title = this.getTitle();
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.drawerList = (ListView) findViewById(R.id.left_drawer);

        this.homeHolder = (LinearLayout) findViewById(R.id.home_container);
        this.fragHolder = (FrameLayout) findViewById(R.id.frag_container);

        // Set the adapter for the list view
        this.drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, section_titles));

        this.drawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.drawable.ic_drawer, R.drawable.ic_drawer)
        {
            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view)
            {
                super.onDrawerClosed(view);
                MainActivity2.this.getSupportActionBar().setTitle(MainActivity2.this.current_title);
                unHighlightSelection();
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                // MainActivity2.this.getSupportActionBar().setTitle("To_find_a_title");//TODO find a better title!!!!!
            }
        };

        this.drawerLayout.setDrawerListener(this.drawerToggle);
        this.drawerList.setOnItemClickListener(new DrawerItemClickListener());

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

        this.selectItem(0);

        this.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));

        srl = (MSwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        srl.setOnRefreshListener(this);
        srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener()
        {
            @Override
            public boolean canChildScrollUp()
            {
                List<Fragment> frags = getSupportFragmentManager().getFragments();

                if(frags.size() < 1) return false;

                RecyclerView rv = null;

                for(int i = 0; i < frags.size(); i++)
                {
                    if (frags.get(i) instanceof FlagsListFragment)
                    {
                        rv = ((FlagsListFragment) frags.get(i)).getRV();
                        break;
                    }
                }

                if(rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();

                // Log.d(TAG, "First completely visible item position: " + position);

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });

        Fragment fragment = new FlagsListFragment();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.swipe_refresh, fragment).commit();

        SupportMapFragment mapFragment = new SupportMapFragment();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.map_holder, mapFragment).commit();
        mapFragment.getMapAsync(this);

        this.receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                switch(intent.getAction()) {
//                    TODO first work towards implementing a fragment switch when flaglist is empty
                    case LocationService.FOUND_NEW_FLAGS_NOTIFICATION:
//                        Fragment listFragment = new FlagsListFragment();
//                        MainActivity2.this.getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.swipe_refresh, listFragment).commit();
                        break;

                    case LocationService.FOUND_NO_FLAGS_NOTIFICATION:
//                        NoFlagsFragment noFlagsFragment = new NoFlagsFragment();
//                        MainActivity2.this.getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.swipe_refresh, noFlagsFragment).commit();
                        break;

                    default:
                }
                updateMarkersOnMap();
            }
        };
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.receiver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        this.gMap = googleMap;

        this.gMap.getUiSettings().setScrollGesturesEnabled(false);
        this.gMap.getUiSettings().setZoomGesturesEnabled(false);
        this.gMap.setMyLocationEnabled(true);

        LocalBroadcastManager.getInstance(this).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
        LocalBroadcastManager.getInstance(this).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NO_FLAGS_NOTIFICATION));


        this.updateMarkersOnMap();
    }

    public void updateMarkersOnMap()
    {
        List<Flag> pins= PlacesApplication.getInstance().getFlags();

        if(pins != null && this.gMap != null)
        {
            this.gMap.clear();

            //zooms around all the Flags
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (ParseObject p : pins)
            {
                Flag f = (Flag) p;
                ParseGeoPoint location = f.getLocation();
                String text = f.getText();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                builder.include(latLng);

                //25% size original icon
                int marker_id = getIconForCategory(f.getCategory());
                Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                                            (marker,
                                            (int)(marker.getWidth() * 0.25f),
                                            (int)(marker.getHeight() * 0.25f),
                                            false);

                this.gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(text)
                        .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                        .alpha(0.8f));
            }

            if(pins.size() > 0)
            {
                LatLngBounds bounds = builder.build();
//                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUNDS));
                this.gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUNDS));
            }else{
                Location currentLocation = gMap.getMyLocation();
                if(currentLocation != null){
                    LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    this.gMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentLocationLatLng, ZOOM_LVL));
                }
            }

        }
    }

    private int getIconForCategory(String category)
    {
        String[] category_array = this.getResources().getStringArray(R.array.categories);

        if (category == null || category.equals(category_array[0])) return R.drawable.flag_red;
        else if (category.equals(category_array[1])) return R.drawable.flag_green;
        else if (category.equals(category_array[2])) return R.drawable.flag_yellow;
        else if (category.equals(category_array[3])) return R.drawable.flag_blue;
        else return R.drawable.flag_purple; // 'Food' category
    }

    private float getCategoryColor(String category)
    {
        String[] category_array = this.getResources().getStringArray(R.array.categories);

        if (category == null || category.equals(category_array[0])) return BitmapDescriptorFactory.HUE_RED;
        else if (category.equals(category_array[1])) return BitmapDescriptorFactory.HUE_AZURE;
        else if (category.equals(category_array[2])) return BitmapDescriptorFactory.HUE_ORANGE;
        else if (category.equals(category_array[3])) return BitmapDescriptorFactory.HUE_BLUE;
        else return BitmapDescriptorFactory.HUE_MAGENTA; // 'Food' category
    }

    @Override
    public void onRefresh()
    {
        refresh();
        srl.setRefreshing(false);
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        this.drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event

        if (drawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        else if(item.getItemId() == R.id.action_add_flag)
        {
            Intent shareIntent = new Intent(this, ShareActivity.class);
            startActivityForResult(shareIntent, MainActivity2.SHARE_ACTIVITY_REQUEST_CODE);
            // item.setVisible(false);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
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

    @Override
    public void onPause()
    {
        super.onPause();

        isForeground = false;

    }

    public static boolean isForeground(){
        return isForeground;
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

    private void logout()
    {
        // Log the user out
        ParseUser.logOut();

        FacebookUtils.getInstance().clearUserData();

        // Go to the login view
        FacebookUtils.startLoginActivity(this);
    }



    /** Swaps fragments in the main content view */
    private void selectItem(int position)
    {
        // TODO I have temporarily removed the block of code below for my implementation to work
        /*
        if(this.currentDrawerListItemIndex == position)
        {
            this.drawerLayout.closeDrawers();

            return;
        }
        */

        if(position == SETTINGS_POSITION)
        {
            // startActivityForResult(new Intent(this, SettingsActivity.class), SHARE_ACTIVITY_REQUEST_CODE);
            switchToSettingsFrag();
        }
        else if(position == LOGOUT_POSITION)
        {
            logout();
        }
        else if(position == FLAGS_LIST_POSITION)
        {
            // Fragment fragment = new FlagsListFragment();
            // this.getSupportFragmentManager().beginTransaction().replace(R.id.swipe_refresh, fragment).commit();

            if(homeHolder.getVisibility() == View.INVISIBLE) switchToListMapFrags();
        }

        // this.drawerList.setItemChecked(position, true);
        this.drawerLayout.closeDrawers();

        // this.currentDrawerListItemIndex = position;

        /*
        // Highlight the selected item, update the title, and close the drawer
        this.drawerList.setItemChecked(position, true);
        this.setTitle(MainActivity2.section_titles[position]);
        this.drawerLayout.closeDrawer(this.drawerList);

        this.selected_item_index = position;
        */
    }

    private void unHighlightSelection()
    {
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
            MainActivity2.this.selectItem(position);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case Utils.LOGIN_REQUEST_CODE:

                if(resultCode == RESULT_OK){
                    //this.startDownloadingFacebookInfo();
                    FacebookUtils.downloadFacebookInfo(this);
                }
                break;
            case SHARE_ACTIVITY_REQUEST_CODE:

                if(resultCode == RESULT_OK){
                    Toast.makeText(this, data.getExtras().getString("result"), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void getRidOfUnusedFrag()
    {
        android.app.Fragment frag1 = this.getFragmentManager().findFragmentByTag(FRAG_TAG);
        Fragment frag2 = this.getSupportFragmentManager().findFragmentByTag(FRAG_TAG);

        // TODO We need to check if we need both lines since we got both "fragments" and "support fragments"
        if(frag1 != null) this.getFragmentManager().beginTransaction().remove(frag1).commit();
        else if(frag2!= null) this.getSupportFragmentManager().beginTransaction().remove(frag2).commit();
    }

    private void switchToFragOtherThanHome()
    {
        homeHolder.setVisibility(View.INVISIBLE);
        fragHolder.setVisibility(View.VISIBLE);
    }

    private void switchToListMapFrags()
    {
        getRidOfUnusedFrag();

        homeHolder.setVisibility(View.VISIBLE);
        fragHolder.setVisibility(View.INVISIBLE);
    }

    private void switchToSettingsFrag()
    {
        getRidOfUnusedFrag();
        switchToFragOtherThanHome();

        this.getFragmentManager().beginTransaction().replace(R.id.frag_container, new SettingsFragment(), FRAG_TAG).commit();
    }


    protected void switchToFlagFrag(Fragment frag)
    {
        getRidOfUnusedFrag();
        switchToFragOtherThanHome();

        this.getSupportFragmentManager().beginTransaction().replace(R.id.frag_container, frag, FRAG_TAG).commit();
    }

    @Override
    public void onBackPressed()
    {
        if(homeHolder.getVisibility() == View.INVISIBLE)
        {
            switchToListMapFrags();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        if(preference.getKey().equals("meFilter") ||
                preference.getKey().equals("flFilter") ||
                preference.getKey().equals("strangersFilter") ||
                preference.getKey().equals("timeFilter") ||
                preference.getKey().equals("thoughtsCheck") ||
                preference.getKey().equals("funCheck") ||
                preference.getKey().equals("musicCheck") ||
                preference.getKey().equals("landscapeCheck") ||
                preference.getKey().equals("foodCheck") ||
                preference.getKey().equals("noneCheck"))
        {
            Log.d(TAG, "Called onPreferenceChange for: " + preference.getKey());
            editor.putBoolean(preference.getKey(), (boolean)newValue);
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
        else if(preference.getKey().equals("maxFetch"))
        {
            preference.setDefaultValue(newValue);
            int value = Utils.stepValues[(int)newValue];
            Utils.MAX_PINS = value;
            showToast("Max number of visible flags: " + value + '.');
            Log.d(TAG, "SeekBar changed! New radius value: " + Utils.MAP_RADIUS);
        }

        Location currentLocation = PlacesApplication.getInstance().getLocation();
        PlacesApplication.getInstance().getLocationService().queryParsewithLocation(currentLocation);

        return true;
    }

    private void showToast(String text) {
        if(radiusToast != null)
            radiusToast.cancel();
        radiusToast = Toast.makeText(getBaseContext(), text,
                Toast.LENGTH_SHORT);
        radiusToast.show();
    }

}





