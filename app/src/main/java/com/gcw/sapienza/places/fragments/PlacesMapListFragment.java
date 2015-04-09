package com.gcw.sapienza.places.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.utils.CropCircleTransformation;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesUtilCallback;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Abstract class for showing Fragment with Map and list of flags.
 * Subclasses must implement all abstract methods providing the right data
 *
 * Created by paolo on 26/03/15.
 */
public abstract class PlacesMapListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "PlacesMapListFragment";

    protected FlagsListFragment listFragment;
    private MSwipeRefreshLayout srl;
    private FlagsAdapter flagsAdapter;
    private BroadcastReceiver flagsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onFlagsReceived(context, intent);
            List<Flag> data = PlacesMapListFragment.this.getData();
            PlacesMapListFragment.this.listFragment.flagsToDisplay();
            PlacesMapListFragment.this.flagsAdapter.updateData(data);

        }
    };
    private BroadcastReceiver noFlagsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onNoFlagsReceived(context, intent);
            PlacesMapListFragment.this.listFragment.noFlagsToDisplay(noFlagsReceivedText());
        }
    };

    protected abstract String noFlagsReceivedText();

    /**
    * @param context The Context in which the flagsReceiver is running.
    * @param intent The Intent being received.
    */
    protected abstract void onFlagsReceived(Context context, Intent intent);

    /**
     * @param context The Context in which the noFlagsReceiver is running.
     * @param intent The Intent being received.
     */
    protected abstract void onNoFlagsReceived(Context context, Intent intent);

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    protected abstract Collection<IntentFilter> getFlagsFilters();


    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    protected abstract Collection<IntentFilter> getNoFlagsFilters();

    /**
     * This method is called when new data is needed
     */
    protected abstract void handleRefreshData();

    /**
     * Returns the list of flags for current subclass
     * @return list of flags
     */
    protected abstract List<Flag> getData();


    /**
     *
     * @return true if you want to enable discover mode on map
     */
    protected abstract boolean showDiscoverModeOnMap();


    @Override
    public void onRefresh(){
        this.handleRefreshData();
        srl.setRefreshing(false);
    }


    /**
     * Registers the flagsReceiver from local notifications
     */
    private void registerNotification(){
        Collection<IntentFilter> flagsFilters = this.getFlagsFilters();
        for(IntentFilter filter : flagsFilters){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.flagsReceiver, filter);
        }
        Collection<IntentFilter> noFlagsFilters = this.getNoFlagsFilters();
        for(IntentFilter filter : noFlagsFilters){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.noFlagsReceiver, filter);
        }
    }
    /**
     * Unregisters the flagsReceiver from local notifications
     */
    private   void unregisterNotification(){
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.flagsReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.noFlagsReceiver);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.registerNotification();
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_flags_layout, container, false);


        this.srl = (MSwipeRefreshLayout) view.findViewById(R.id.my_swipe_refresh);
        this.srl.setOnRefreshListener(this);


        this.srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener() {
            @Override
            public boolean canChildScrollUp() {


                FlagsListFragment f = PlacesMapListFragment.this.listFragment;
                if(f == null){
                    return false;
                }
                RecyclerView rv = f.getRecyclerView();

                if (rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();

                // Log.d(TAG, "First completely visible item position: " + position);

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });


        this.listFragment = new FlagsListFragment();
        if(this.flagsAdapter == null){
            this.flagsAdapter = new FlagsAdapter(this.getData(), this.getActivity());
        }
        this.listFragment.setInitialData( this.flagsAdapter);

        this.handleRefreshData();

        this.getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.my_swipe_refresh, this.listFragment).commit();

        /*SupportMapFragment mapFragment = new SupportMapFragment();
        this.getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.my_map_holder, mapFragment).commit();
        mapFragment.getMapAsync(this);
*/
        return view;
    }

    @Override
    public void onDestroyView() {
        this.unregisterNotification();
        super.onDestroyView();
    }


    /**
     * Customizes GMap UI. Subclasses can implement this method to change the UiSettings of GMaps
     * When implementing in subclasses remember to call super.customizeGmap()
     * @param googleMap the Gmap
     */
    protected void customizeGmap(GoogleMap googleMap){
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMyLocationEnabled(true);
    }





    /**
     * check if discover mode is enabled
     *
     * @return true if discover mode is enabled, false otherwise
     */
    private static boolean isDiscoverModeEnabled() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlacesApplication.getPlacesAppContext());

        return preferences.getBoolean("discoverMode", true);

    }










    private class FlagsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private static final String TAG = "FlagsAdapter";
        private List<Flag> flags;
        private Context context;
        private final Activity mainActivity;
        private final Transformation transformation = new CropCircleTransformation();
        private MapViewHolder mapViewHolder;

        private int selectedFlagIndex;


        public FlagsAdapter(List<Flag> list, Activity mainActivity) {
            this.flags = list;
            this.mainActivity = mainActivity;
        }

        public void setContext(Context c){
            this.context = c;
        }

        public void updateData(List<Flag> flags){
            this.flags = flags;
            this.notifyDataSetChanged();
            this.mapViewHolder.updateMarkersOnMap();
        }

        public Flag getSelectedFlag() {
            try {
                return flags.get(selectedFlagIndex);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        public void setSelectedFlagIndex(int i) {
            selectedFlagIndex = i;
        }


        private void onBindFlag(final FlagsViewHolder flagViewHolder, Flag f){
            flagViewHolder.setCurrentFlag(f);
            flagViewHolder.flagAdapter = this;

            if (f.getPassword() == null) {
                String text = f.getText();
                if (text != null && text.length() > 0) {
                    flagViewHolder.main_text.setVisibility(View.VISIBLE);
                    flagViewHolder.main_text.setText(f.getText());
                } else {
                    flagViewHolder.main_text.setVisibility(View.GONE);
                }


            }
            else {
                flagViewHolder.main_text.setText("***Private flag***");
            }


            String user_id = f.getFbId();
            String account_type = f.getAccountType();
            String fb_username = f.getFbName(); // checks if Flag has fb username. if there is one use it otherwise ask FB

            // If user is logged in with G+, FB Graph API cannot be used
            if (fb_username == null)
                PlacesLoginUtils.getInstance().loadUsernameIntoTextView(user_id, flagViewHolder.username, f.getAccountType());
            else
            {
                if(!PlacesLoginUtils.getInstance().getUserIdMap().containsKey(user_id)) PlacesLoginUtils.getInstance().addEntryToUserIdMap(user_id, fb_username);
                flagViewHolder.username.setText(fb_username);
            }


            PlacesLoginUtils.getInstance().getProfilePictureURL(user_id, account_type, PlacesLoginUtils.PicSize.SMALL, new PlacesUtilCallback() {
                @Override
                public void onResult(String result, Exception e) {

                    if (result != null && !result.isEmpty()) {

                        Picasso.with(FlagsAdapter.this.context).load(result).transform(transformation).into(flagViewHolder.user_profile_pic);
                    }
                }
            });

            int numberOfWows = f.getWowCount();
            flagViewHolder.stats_wow.setText(numberOfWows + " WoW");


            int numberOfComments = f.getNumberOfComments();
            if (numberOfComments == 1) {
                flagViewHolder.stats_comment.setText(numberOfComments + " comment");
            } else {
                flagViewHolder.stats_comment.setText(numberOfComments + " comments");
            }

            ParseFile pic = f.getThumbnail();
            if (pic != null && f.getPassword() == null) {
                String url = pic.getUrl();
                flagViewHolder.main_image.setVisibility(View.VISIBLE);
                flagViewHolder.main_image.setClickable(false);
                Picasso.with(this.context).load(url).fit().centerCrop().into(flagViewHolder.main_image);

            } else {
                flagViewHolder.main_image.setVisibility(View.GONE);
                flagViewHolder.main_image.setImageDrawable(null);
            }


            ((CardView) flagViewHolder.itemView).setShadowPadding(0, 0, 7, 7);

            //working on making icons or symbols which represents a category

            String[] category_array = PlacesApplication.getInstance().getResources().getStringArray(R.array.categories);

            if (flagViewHolder.mFlag.getCategory().equals(category_array[0])) { //None
                flagViewHolder.categoryIcon.setImageResource(R.drawable.none);
            } else if (flagViewHolder.mFlag.getCategory().equals(category_array[1])) { //Thoughts
                flagViewHolder.categoryIcon.setImageResource(R.drawable.thoughts);
            } else if (flagViewHolder.mFlag.getCategory().equals(category_array[2])) { //Fun
                flagViewHolder.categoryIcon.setImageResource(R.drawable.smile);
            } else if (flagViewHolder.mFlag.getCategory().equals(category_array[3])) { //Music
                flagViewHolder.categoryIcon.setImageResource(R.drawable.music);
            } else if (flagViewHolder.mFlag.getCategory().equals(category_array[4])) { //Landscape
                flagViewHolder.categoryIcon.setImageResource(R.drawable.eyes);
            } else {
                flagViewHolder.categoryIcon.setImageResource(R.drawable.food);
            }

        }

        private void onBindMapViewHolder(MapViewHolder mapViewHolder){
            //do nothing
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int i) {
            if( i == 0){
                this.onBindMapViewHolder((MapViewHolder)viewHolder);
            }
            else{
                Flag f = this.flags.get(i - 1);
                this.onBindFlag((FlagsViewHolder)viewHolder, f);
            }


        }

        @Override
        public int getItemCount() {
            if (this.flags == null) return 0;
            else return this.flags.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            // This is brilliant, I can explain
            //return position;

            //This must be the same value for each view otherwise the RecyclerView will perform badly.
            return position == 0 ? 0 : 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            if(i == 0){
                View itemView = LayoutInflater.
                        from(viewGroup.getContext()).
                        inflate(R.layout.card_map_layout, viewGroup, false);
                MapViewHolder mapViewHolder = new MapViewHolder(itemView, mainActivity);
                this.mapViewHolder = mapViewHolder;
                return mapViewHolder;
            }
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.card_layout, viewGroup, false);

            return new FlagsAdapter.FlagsViewHolder(itemView, mainActivity);
        }


        public class MapViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
            private static final String TAG = "MapViewHolder";
            protected GoogleMap gMap;
            protected final Activity activity;
            protected ArrayList<Marker> markers;

            public MapViewHolder(View v, Activity context){
                super(v);
                this.activity = context;
                MapFragment fragment = MapFragment.newInstance();
                FragmentTransaction fragmentTransaction =
                        context.getFragmentManager().beginTransaction();
                fragmentTransaction.add(R.id.contents_container, fragment);
                fragmentTransaction.commit();
                fragment.getMapAsync(this);
            }

            @Override
            public void onMapReady(GoogleMap googleMap) {
                this.gMap = googleMap;
                this.gMap.setOnMarkerClickListener(this);
                PlacesMapListFragment.this.customizeGmap(googleMap);
                this.updateMarkersOnMap();
            }


            @Override
            public boolean onMarkerClick(Marker selectedMarker) {
                int index = Integer.parseInt(selectedMarker.getSnippet());

                PlacesMapListFragment.this.listFragment.getRecyclerView().smoothScrollToPosition(index);


                if(selectedMarker.getAlpha() == Utils.FLAG_ALPHA_SELECTED){
                    for (Marker marker : this.markers) {
                        marker.setAlpha(Utils.FLAG_ALPHA_NORMAL);
                    }
                }else {
                    for (Marker marker : this.markers) {
                        marker.setAlpha(Utils.FLAG_ALPHA_UNSELECTED);
                    }
                    selectedMarker.setAlpha(Utils.FLAG_ALPHA_SELECTED);
                }

                // by returning false we can show text on flag in the map
                // return false;
                return true;
            }



            /**
             * Puts markers on the map
             */
            private void updateMarkersOnMap() {

                this.markers = new ArrayList<>();

                List<Flag> flags = FlagsAdapter.this.flags;

                Log.d(TAG, "Updating markers on map...flags size: " + flags.size());

                if ( this.gMap != null) {
                    this.gMap.clear();

                    //zooms around all the Flags
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    int index = 0;

                    for (Flag f : flags) {
                        ParseGeoPoint location = f.getLocation();
                        String text = f.getText();
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        builder.include(latLng);

                        //25% size original icon
                        int marker_id = Utils.getIconForCategory(f.getCategory(), this.activity);
                        Bitmap marker = BitmapFactory.decodeResource(this.activity.getResources(), marker_id);
                        Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                                (marker,
                                        (int) (marker.getWidth() * Utils.FLAG_SCALE_NORMAL),
                                        (int) (marker.getHeight() * Utils.FLAG_SCALE_NORMAL),
                                        false);

                        Marker newMarker = this.gMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(text)
                                .snippet( String.valueOf(index))
                                .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                        // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                        //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                                .alpha(Utils.FLAG_ALPHA_NORMAL));
                        this.markers.add(newMarker);
                        index++;
                    }

                if (PlacesMapListFragment.this.showDiscoverModeOnMap() && PlacesMapListFragment.isDiscoverModeEnabled()) {

                    List<Flag> hiddenFlags = FlagsStorage.getSharedStorage().fetchFlagsWithType(FlagsStorage.Type.HIDDEN);

                    Log.d(TAG, "Hidden Flags size: " + hiddenFlags.size());

                    for (ParseObject hf : hiddenFlags) {
                        Flag f = (Flag) hf;
                        ParseGeoPoint location = f.getLocation();
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        builder.include(latLng);

                        //25% size original icon
                        int marker_id = Utils.getIconForCategory(f.getCategory(), getActivity());
                        Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                        Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                                (marker,
                                        (int) (marker.getWidth() * Utils.FLAG_SCALE_NORMAL),
                                        (int) (marker.getHeight() * Utils.FLAG_SCALE_NORMAL),
                                        false);

                        this.gMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("**DiscoverMode**\nget closer to see the content")
                                .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                        // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                        //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                                .alpha(0.25f));
                    }

                }

                    Location currentLocation = gMap.getMyLocation();

                    if (currentLocation != null) {
                        LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        builder.include(currentLocationLatLng);
                        this.gMap.addCircle(new CircleOptions()
                                .center(currentLocationLatLng)
                                .radius(Utils.MAP_RADIUS * 1000)
                                .strokeColor(Color.GREEN)
                                .fillColor(Color.TRANSPARENT));
                    }

                    if(flags.size() > 0){
                        final LatLngBounds bounds = builder.build();
                        try {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                        } catch (IllegalStateException ise) {
                            this.gMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                                private boolean changed = false;
                                @Override
                                public void onCameraChange(CameraPosition cameraPosition) {
                                    if (changed) return;
                                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                                    changed = true;
                                    gMap.setOnCameraChangeListener(null);

                                }
                            });
                        }
                    }
                    else {
                        if (currentLocation != null) {
                            LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                                    currentLocation.getLongitude());
                            this.gMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(currentLocationLatLng, Utils.ZOOM_LVL));
                        }
                    }

                }
            }


        }

        public class FlagsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

            protected final TextView main_text;
            protected final TextView username;
            protected final TextView stats_wow;
            protected final TextView stats_comment;
            protected final ImageView user_profile_pic;
            protected final ImageView main_image;
            private final Activity mainActivity;

            //new view for icon or line representing the category
            protected ImageView categoryIcon;
            protected FlagsAdapter flagAdapter;
            private String password;
            private Flag mFlag;
            private int numberOfWows;

            public FlagsViewHolder(View v, Activity context) {
                super(v);

                this.mainActivity = context;

                v.setOnClickListener(this);

                this.main_image = (ImageView) v.findViewById(R.id.card_imageView_image);
                this.user_profile_pic = (ImageView) v.findViewById(R.id.card_profile_pic);
                this.username = (TextView) v.findViewById(R.id.card_textView_username);
                this.main_text = (TextView) v.findViewById(R.id.card_textView_text);
                this.stats_wow = (TextView) v.findViewById(R.id.stats_wows);
                this.stats_comment = (TextView) v.findViewById(R.id.stats_comments);

                //new for icon representing category, or a colored line
                this.categoryIcon = (ImageView) v.findViewById(R.id.categoryIcon);

                v.setOnCreateContextMenuListener(this);
            }

            public void setCurrentFlag(Flag flag) {
                this.mFlag = flag;
            }

            @Override
            public void onClick(View v) {
                if (mFlag.getPassword() != null) askForPassword(mFlag.getPassword());
                else openFlag();
            }

            private void openFlag() {

                FlagFragment frag = new FlagFragment();
                frag.setFlag(this.mFlag);

                ((MainActivity) mainActivity).switchToOtherFrag(frag);
            }

            private void askForPassword(final String psw) {
                LayoutInflater li = LayoutInflater.from(mainActivity);
                View passwordDialogLayout = li.inflate(R.layout.password_dialog, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
                alertDialogBuilder.setView(passwordDialogLayout);

                final EditText userInput = (EditText) passwordDialogLayout.findViewById(R.id.password_field);

                // final InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                alertDialogBuilder
                        .setCancelable(true)
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                })
                        .setPositiveButton("Confirm",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        password = userInput.getText().toString();
                                        if (!password.equals(psw)) {
                                            Toast.makeText(mainActivity, "Wrong password", Toast.LENGTH_LONG).show();
                                            userInput.setText("");
                                        } else {
                                            dialog.dismiss();

                                            openFlag();
                                        }
                                    }

                                }

                        );

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                // inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                // inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//            flagsAdapter.setSelectedFlagIndex(getPosition());
                menu.setHeaderTitle("Edit");
                flagAdapter.setSelectedFlagIndex(getPosition());
                Log.d(TAG, "Item position: " + getPosition());
                String fb_id = mFlag.getFbId();
                //        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                //        Flag sel_usr = (Flag)(recycleView.getItemAtPosition(info.position));
                //        String fb_id = sel_usr.getFbId();
                //
                if (PlacesLoginUtils.getInstance().getCurrentUserId().equals(fb_id)) {
                    menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_FLAG, 0, "Delete Flag");
                } else {
                    Log.d(TAG, "Username: " + ParseUser.getCurrentUser().getUsername());
                    Log.d(TAG, "objectId: " + mFlag.getObjectId());

                    ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

                    queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
                    queryDelete.whereEqualTo("reported_flag", mFlag);

                    try {
                        if (queryDelete.count() == 0) {
                            menu.add(Utils.FLAG_LIST_GROUP, Utils.REPORT_FLAG, 0, "Report Flag as inappropriate");
                        } else {
                            menu.add(Utils.FLAG_LIST_GROUP, Utils.DELETE_REPORT_FLAG, 0, "Revoke Flag report");
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }

            //this is as updateWowInfo in the FlagFragment, but only for wow and used for counter in the
            //card view, useful when we will have a button instead of a textView
            private void updateWowCounter() { //like update
                ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
                queryPosts.whereEqualTo("objectId", mFlag.getObjectId());

                queryPosts.findInBackground(new FindCallback<Flag>() {
                    @Override
                    public void done(List<Flag> markers, ParseException e) {
                        if (e == null && markers.size() != 0) {
                            Flag flag = markers.get(0);

                            numberOfWows = flag.getInt("wowCount");
                        }
                    }
                });

                stats_wow.setText(numberOfWows + " WoW");
            }


        }


    }

















    /**
     * Fragment that shows the list of flags in a cardview
     */
    public static class FlagsListFragment extends Fragment{

        private RecyclerView recycleView;
        private RelativeLayout noFlagLayout;
        private TextView noFlagsText;

        private FlagsAdapter adapter;

        protected void flagsToDisplay(){
            noFlagLayout.setVisibility(View.GONE);
            recycleView.setVisibility(View.VISIBLE);
        }

        protected void noFlagsToDisplay(String text){
            noFlagsText.setText(text);
            recycleView.setVisibility(View.GONE);
            noFlagLayout.setVisibility(View.VISIBLE);
        }

        public void setInitialData(FlagsAdapter adapter){
            this.adapter = adapter;
        }

        public RecyclerView getRecyclerView() {
            return recycleView;
        }


        private void setFlagsAdapter(FlagsAdapter adapter){
            adapter.setContext(this.recycleView.getContext());
            this.recycleView.setAdapter(adapter);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

            this.recycleView = (RecyclerView) view.findViewById(R.id.cardList);
            LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            this.recycleView.setLayoutManager(llm);

            noFlagLayout = (RelativeLayout)view.findViewById(R.id.no_flags_found_layout);
            noFlagsText = (TextView)noFlagLayout.findViewById(R.id.no_flags_text);

            if(this.adapter == null){
                throw new RuntimeException("You must set initial data before adding this fragment");
            }
            this.setFlagsAdapter(this.adapter);


            registerForContextMenu(recycleView);

            return view;
        }



        @Override
        public boolean onContextItemSelected(MenuItem item) {
            FlagsAdapter fa = (FlagsAdapter) recycleView.getAdapter();
            Flag sel_usr = fa.getSelectedFlag();

            if (sel_usr == null)
                Toast.makeText(getActivity(), Utils.NO_VALID_FLAG_SELECTED, Toast.LENGTH_SHORT).show();

            switch (item.getItemId()) {

                case Utils.DELETE_FLAG:
                    this.deleteFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                case Utils.REPORT_FLAG:
                    this.reportFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                case Utils.DELETE_REPORT_FLAG:
                    this.deleteReportFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                default:
                    fa.setSelectedFlagIndex(-1);
                    return super.onContextItemSelected(item);
            }
        }

        /**
         * Deletes the comment
         *
         * @param f flag to delete
         */
        private void deleteFlag(Flag f) {
            f.deleteInBackground(new DeleteCallback() {
                @Override
                public void done(com.parse.ParseException e) {
                    if (e == null) {
                        Toast.makeText(recycleView.getContext(), Utils.FLAG_DELETED, Toast.LENGTH_SHORT).show();
                        ((MainActivity) getActivity()).refresh(Utils.NEARBY_FLAGS_CODE);
                    } else
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Reports a flag
         *
         * @param f flag to report
         */
        private void reportFlag(final Flag f) {

            FlagReport report = FlagReport.createFlagReportFromFlag(f);
            report.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(recycleView.getContext(), Utils.FLAG_REPORTED, Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Deletes an entry from the Reported_Posts table
         *
         * @param f flag related to the entry to be deleted
         */
        private void deleteReportFlag(Flag f) {
            ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

            queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
            queryDelete.whereEqualTo("reported_flag", f);

            queryDelete.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject p, ParseException e) {
                    if (e == null) {
                        p.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(recycleView.getContext(), Utils.FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }



    }


}
