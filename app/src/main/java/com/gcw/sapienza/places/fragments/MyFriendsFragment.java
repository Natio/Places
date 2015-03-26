package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.adapters.FriendsListAdapter;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by snowblack on 3/1/15.
 */
public class MyFriendsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "MyFriendsFragment";

    private ListView friendsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_friends_layout, container, false);
        friendsListView = (ListView) view.findViewById(R.id.friends_list_view);

//        adapter = new ArrayAdapter<String>(PlacesApplication.getPlacesAppContext(), R.layout.custom_spinner, PlacesLoginUtils.getInstance().getFriends());
//
//        friendsList.setAdapter(adapter);

        List<String> friendListFbId = PlacesLoginUtils.getInstance().getFriends();

        friendsListView.setAdapter(new FriendsListAdapter(getActivity(), R.layout.friend_layout, friendListFbId));
        friendsListView.setOnItemClickListener(this);
        return view;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Click");
        String fbid =(String) friendsListView.getAdapter().getItem(position);

        ParseQuery<PlacesUser> queryUsers = ParseQuery.getQuery("_User");
        queryUsers.whereEqualTo(PlacesUser.FACEBOOK_ID_KEY, fbid);
        queryUsers.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);

        try{
            //the following query is not executed in background
            //because the user is cached
            PlacesUser user = queryUsers.getFirst();
            ((MainActivity) this.getActivity()).switchToOtherFrag(ProfileFragment.newInstance(user));

        }
        catch (ParseException ex){
            Log.e(TAG, "error", ex);
        }

    }
}
