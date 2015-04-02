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
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.List;

/**
 * Created by snowblack on 3/30/15.
 */
public class InboxFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "InboxFragment";

    private ListView inboxListView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.inbox_layout, container, false);
        inboxListView = (ListView) view.findViewById(R.id.friends_list_view);

        try {
            List<List<String>> inbox = PlacesStorage.fetchInbox(getActivity());
            inboxListView = (ListView) view.findViewById(R.id.inbox_list_view);
            inboxListView.setAdapter(new InboxAdapter(getActivity(), R.layout.inbox_message_layout, inbox));
            inboxListView.setOnItemClickListener(this);
            return view;
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Utils.showToast(getActivity(), "Something went wrong while loading your Inbox", Toast.LENGTH_SHORT);
        }

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Click");
        String fbid =(String) inboxListView.getAdapter().getItem(position);

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
