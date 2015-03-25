package com.gcw.sapienza.places.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.gcw.sapienza.places.models.FlagComparator;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Collections;
import java.util.List;

/**
 * Created by paolo  on 10/01/15.
 */
public class FlagsListFragment extends Fragment {

    private static final String TAG = "FlagsListFragment";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                //handled in the same way
                case LocationService.FOUND_NEW_FLAGS_NOTIFICATION:
                    noFlagLayout.setVisibility(View.GONE);
                    recycleView.setVisibility(View.VISIBLE);
                    FlagsListFragment.this.updateRecycleViewWithNewContents(Utils.getOrderedFlags(getActivity(), Utils.NEARBY_FLAGS_CODE));
                    break;
                case LocationService.FOUND_NO_FLAGS_NOTIFICATION:
                    noFlagsText.setText("Sigh! No Flags nearbly (yet!) :(");
                    recycleView.setVisibility(View.GONE);
                    noFlagLayout.setVisibility(View.VISIBLE);
                    break;

                default:
                    Log.w(TAG, intent.getAction() + ": cannot identify the received notification");
            }
            Log.d(TAG, intent.getAction());
        }
    };

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

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NO_FLAGS_NOTIFICATION));



        this.updateRecycleViewWithNewContents(Utils.getOrderedFlags(getActivity(), Utils.NEARBY_FLAGS_CODE));

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
                    ((MainActivity) getActivity()).refresh();
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

