package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.adapters.FriendsListAdapter;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;

import java.util.List;

/**
 * Created by snowblack on 3/1/15.
 */
public class MyFriendsFragment extends Fragment {

    private static final String TAG = "MyFriendsFragment";

    private View view;
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

        return view;
    }


}
