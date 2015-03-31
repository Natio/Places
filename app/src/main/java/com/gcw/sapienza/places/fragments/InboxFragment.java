package com.gcw.sapienza.places.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.Utils;

import java.io.IOException;
import java.util.List;

/**
 * Created by snowblack on 3/30/15.
 */
public class InboxFragment extends Fragment {
    private static final String TAG = "InboxFragment";

    private ListView inboxListView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.inbox_layout, container, false);
        inboxListView = (ListView) view.findViewById(R.id.friends_list_view);

        try {
            List<List<String>> inbox = PlacesStorage.fetchInbox(getActivity());
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Utils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        }

        return view;
    }
}
