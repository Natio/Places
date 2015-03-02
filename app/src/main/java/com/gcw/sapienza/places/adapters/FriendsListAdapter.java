package com.gcw.sapienza.places.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.gcw.sapienza.places.utils.PlacesLoginUtils;

import java.util.List;

/**
 * Created by snowblack on 3/2/15.
 */
public class FriendsListAdapter implements ListAdapter {

    private static final String TAG = "FriendsListAdapter";

    private View view;
    private Context context;

    List<String> friendsList;

    public FriendsListAdapter(View view, Context context) {

        this.view = view;
        this.context = context;

        friendsList = PlacesLoginUtils.getInstance().getFriends();
        Log.d(TAG, "Friends list size: " + friendsList.size());
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return friendsList.size();
    }

    @Override
    public Object getItem(int position) {
        return friendsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return friendsList.size();
    }

    @Override
    public boolean isEmpty() {
        return friendsList.size() == 0;
    }
}
