package com.gcw.sapienza.places.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.SettingsActivity;
import com.gcw.sapienza.places.ShareActivity;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import java.util.Arrays;
import java.util.List;


public class MainActivity2 extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static String TAG = MainActivity2.class.getName();
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private static final String [] section_titles = {"Home", "Settings", "Logout"};
    private CharSequence current_title;
    private MSwipeRefreshLayout srl;
    private int currentDrawerListItemIndex = -1;

    private static final int FLAGS_LIST_POSITION = 0;
    private static final int SETTINGS_POSITION = 1;
    private static final int LOGOUT_POSITION = 2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ParseFacebookUtils.getSession() != null && ParseFacebookUtils.getSession().isOpened()){
            this.startDownloadingFacebookInfo();
        }
        else{
            this.startLoginActivity();
        }

        PlacesApplication.getInstance().startLocationService();
        setContentView(R.layout.activity_main_drawer_layout);
        this.current_title = this.getTitle();
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        this.drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, section_titles));

        this.drawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.drawable.ic_drawer, R.drawable.ic_drawer){
            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                MainActivity2.this.getSupportActionBar().setTitle(MainActivity2.this.current_title);
                unHighlightSelection();
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
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
                FragmentManager fm = getSupportFragmentManager();
                List<Fragment> frags = fm.getFragments();

                if(frags.size() < 1) return false;

                RecyclerView rv = null;

                for(int i = 0; i < frags.size(); i++)
                {
                    if (frags.get(i) instanceof FlagsListFragment) {
                        rv = ((FlagsListFragment) frags.get(i)).getRV();
                        break;
                    }
                }

                if(rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
                    return position != 0;
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    int[] positions = ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(null);
                    for (int i = 0; i < positions.length; i++) {
                        if (positions[i] == 0) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });
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
            startActivity(new Intent(this, ShareActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlacesApplication.getInstance().startLocationService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_drawer, menu);

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

    private void startLoginActivity()
    {
        if(ParseFacebookUtils.getSession() == null || ParseFacebookUtils.getSession().isClosed())
        {
            ParseLoginBuilder builder = new ParseLoginBuilder(this);

            builder.setParseLoginEnabled(false);

            builder.setFacebookLoginEnabled(true);
            builder.setFacebookLoginPermissions(Arrays.asList("public_profile", "user_friends"/*, "user_relationships", "user_birthday", "user_location"*/));

            // builder.setAppLogo(R.drawable.app_logo);

            startActivityForResult(builder.build(), Utils.LOGIN_REQUEST_CODE);
        }
    }

    private void logout()
    {
        // Log the user out
        ParseUser.logOut();

        FacebookUtils.getInstance().clearUserData();

        // Go to the login view
        startLoginActivity();
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


    /** Swaps fragments in the main content view */
    private void selectItem(int position){

        if(this.currentDrawerListItemIndex == position){
            this.drawerLayout.closeDrawers();
            return;
        }


        if(position == SETTINGS_POSITION)
        {
            startActivity(new Intent(this, SettingsActivity.class));
            this.drawerLayout.closeDrawers();
            return;
        }
        else if(position == LOGOUT_POSITION)
        {
            logout();

            this.drawerLayout.closeDrawers();
            return;
        }
        else if(position == FLAGS_LIST_POSITION){
            Fragment fragment = new FlagsListFragment();
            FragmentManager fragmentManager = this.getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.swipe_refresh, fragment).commit();

        }

        this.currentDrawerListItemIndex = position;


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
                    this.startDownloadingFacebookInfo();
                }
                break;

        }
    }

}





