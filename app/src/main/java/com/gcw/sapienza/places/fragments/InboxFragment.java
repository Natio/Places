/*
 * Copyright 2015-present Places®.
 */

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
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.adapters.InboxAdapter;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;

import java.io.IOException;
import java.util.List;

/**
 * List of all the notification received after someone has commented a flag of mine
 */
public class InboxFragment extends Fragment implements AdapterView.OnItemClickListener{
    private static final String TAG = "InboxFragment";

    private ListView inboxListView;
    //Inbox refreshing currently disabled
//    private MSwipeRefreshLayout inboxSwipe;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.inbox_layout, container, false);

        try {
            List<List<String>> inbox = PlacesStorage.fetchInbox(getActivity());
            inboxListView = (ListView) view.findViewById(R.id.inbox_list_view);
            inboxListView.setAdapter(new InboxAdapter(getActivity(), R.layout.inbox_message_layout, inbox));
            inboxListView.setOnItemClickListener(this);
            return view;
        } catch (IOException e) {
            e.printStackTrace();
            PlacesUtils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            PlacesUtils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        }

        //Inbox refreshing currently disabled
//        inboxSwipe = (MSwipeRefreshLayout)view.findViewById(R.id.inbox_swipe_refresh);

        //FIXME not working, refreshing animation never halts
//        inboxSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                Log.d(TAG, "onRefresh called");
//
//                List<List<String>> inbox = null;
//                try {
//                    inbox = PlacesStorage.fetchInbox(getActivity());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Utils.showToast(getActivity(), "Something went wrong while refreshing Inbox data", Toast.LENGTH_SHORT);
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                    Utils.showToast(getActivity(), "Something went wrong while refreshing Inbox data", Toast.LENGTH_SHORT);
//                }
//                inboxListView.setAdapter(new InboxAdapter(getActivity(), R.layout.inbox_message_layout, inbox));
//                inboxSwipe.setRefreshing(false);
//            }
//        });

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.d(TAG, "Click");
        String flagId = ((InboxAdapter)inboxListView.getAdapter()).getItem(position).get(PlacesStorage.FLAG_POS);

        try {
            PlacesStorage.updateSeenInboxAt(getActivity(), position);
        } catch (IOException e) {
            e.printStackTrace();
            PlacesUtils.showToast(getActivity(), "Something went wrong while updating Inbox data", Toast.LENGTH_SHORT);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            PlacesUtils.showToast(getActivity(), "Something went wrong while updating Inbox data", Toast.LENGTH_SHORT);
        }

        ((MainActivity) this.getActivity()).openFlagFromId(flagId);
    }
}
