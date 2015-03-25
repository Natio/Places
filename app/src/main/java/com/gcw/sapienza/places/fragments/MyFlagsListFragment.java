package com.gcw.sapienza.places.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.Util;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.adapters.FlagsAdapter;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.DeleteCallback;

import java.util.List;

/**
 * Created by paolo  on 10/01/15.
 */
public class MyFlagsListFragment extends Fragment {

    private static final String TAG = "MyFlagsListFragment";
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case LocationService.FOUND_MY_FLAGS_NOTIFICATION:
                    noFlagLayout.setVisibility(View.GONE);
                    recycleView.setVisibility(View.VISIBLE);
                    MyFlagsListFragment.this.updateRecycleViewWithNewContents(Utils.getOrderedFlags(getActivity(), Utils.MY_FLAGS_CODE));
                    break;

                case LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION:
                    noFlagsText.setText("Sigh! No Flags from you (yet!) :(");
                    recycleView.setVisibility(View.GONE);
                    noFlagLayout.setVisibility(View.VISIBLE);
                    break;

                default:
                    Log.w(TAG, intent.getAction() + ": cannot identify the received notification");
            }
        }
    };
    private static final String NO_VALID_FLAG_SELECTED = "No valid Flag selected";
    private static final String FLAG_DELETED = "Flag deleted";
    private static final String FLAG_REPORTED = "Flag reported";
    private static final String FLAG_REPORT_REVOKED = "Flag report revoked";

    private RecyclerView recycleView;
    private RelativeLayout noFlagLayout;
    private TextView noFlagsText;

    public RecyclerView getRV() {
        return recycleView;
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

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_MY_FLAGS_NOTIFICATION));
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION));

        this.updateRecycleViewWithNewContents(Utils.getOrderedFlags(getActivity(), Utils.MY_FLAGS_CODE));

        registerForContextMenu(recycleView);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }

    public void updateRecycleViewWithNewContents(List<Flag> l) {
        this.recycleView.setAdapter(new FlagsAdapter(l, recycleView, getActivity()));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        FlagsAdapter fa = (FlagsAdapter) recycleView.getAdapter();
        Flag sel_usr = fa.getSelectedFlag();

        if (sel_usr == null)
            Toast.makeText(getActivity(), NO_VALID_FLAG_SELECTED, Toast.LENGTH_SHORT).show();

        switch (item.getItemId()) {

            case Utils.DELETE_FLAG:
                this.deleteFlag(sel_usr);
                fa.setSelectedFlagIndex(-1);
                return true;

            default:
                fa.setSelectedFlagIndex(-1);
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Deletes the Flag
     *
     * @param f flag to delete
     */
    private void deleteFlag(Flag f) {
        f.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Toast.makeText(recycleView.getContext(), FLAG_DELETED, Toast.LENGTH_SHORT).show();
                    ((MainActivity) getActivity()).refresh(Utils.MY_FLAGS_CODE);
                } else
                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

