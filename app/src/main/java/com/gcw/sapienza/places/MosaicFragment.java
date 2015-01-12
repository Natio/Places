package com.gcw.sapienza.places;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.adapters.FlagsArrayAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.model.FlagReport;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseFile;
import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MosaicFragment extends Fragment{

    private static final String TAG = "MosaicFragment";

    private static View view;
    private static ListView listView;

    private static TextView textHeader;

    private static FlagsArrayAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.flags_list_layout, container, false);

        //retrieve the listviews
        listView = (ListView)view.findViewById(R.id.flags_list_view);

        SwipeRefreshLayout srl = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh);
        ((MainActivity)getActivity()).setSwipeRefreshLayout(srl);
        srl.setOnRefreshListener((MainActivity)getActivity());

        View header = inflater.inflate(R.layout.header_flags_list, null);
        textHeader = (TextView)header.findViewById(R.id.header);
        textHeader.setText("within " + (int)(Utils.MAP_RADIUS * 1000) + " meters");
        listView.addHeaderView(header);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(Utils.VIBRATION_DURATION);

                if(position == 0)
                {
                    startActivity(new Intent(getActivity().getApplicationContext(), SettingsActivity.class));
                    return;
                }

                Intent intent = new Intent(getActivity().getApplicationContext(), FlagActivity.class);

                Date date = ((Flag) parent.getItemAtPosition(position)).getDate();
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                String sDate = df.format(date);

                Bundle bundle = new Bundle();

                bundle.putString("text", ((Flag) parent.getItemAtPosition(position)).getText());
                bundle.putString("id", ((Flag) parent.getItemAtPosition(position)).getFbId());
                bundle.putString("date", sDate);
                bundle.putString("weather", ((Flag) parent.getItemAtPosition(position)).getWeather());
                bundle.putString("category", ((Flag) parent.getItemAtPosition(position)).getCategory());

                try
                {
                    ParseFile pic_file;
                    if((pic_file = ((Flag) parent.getItemAtPosition(position)).getPic()) != null)
                        bundle.putByteArray("picture", pic_file.getData());

                    ParseFile audio_file;
                    if((audio_file = ((Flag) parent.getItemAtPosition(position)).getAudio()) != null)
                    bundle.putByteArray("audio", audio_file.getData());

                    ParseFile video_file;
                    if((video_file = ((Flag) parent.getItemAtPosition(position)).getVideo()) != null)
                    {
                        File temp = File.createTempFile("places_temp_video", "mp4", getActivity().getCacheDir());
                        temp.deleteOnExit();

                        FileOutputStream outStream = new FileOutputStream(temp);
                        outStream.write(video_file.getData());
                        outStream.close();

                        bundle.putString("video", temp.getPath());
                    }
                }
                catch(IOException ioe){ioe.printStackTrace();}
                catch(com.parse.ParseException pe)
                {
                    Log.v(TAG, "Parse file(s) couldn't be retrieved");
                    pe.printStackTrace();
                }

                intent.putExtras(bundle);

                startActivity(intent);
            }
        });

//        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                if (position == 0) return true;
//                Toast.makeText(getActivity(), "Long Click!", Toast.LENGTH_LONG).show();
//                return true;
//            }
//        });

        loadDefaultSettings();


        if(PlacesApplication.getLocation() != null && adapter == null)
        {
            if(PlacesApplication.mService != null) PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());
            else
            {
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(PlacesApplication.mService == null) handler.postDelayed(this, Utils.UPDATE_DELAY);
                        else PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());
                    }
                });
            }
        }

        registerForContextMenu(listView);

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Edit");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Flag sel_usr = (Flag)(listView.getItemAtPosition(info.position));
        String fb_id = sel_usr.getFbId();

        if(FacebookUtils.getInstance().getCurrentUserId().equals(fb_id)) {
            menu.add(0, Utils.DELETE_POST, 0, "Delete Flag");
        }else {
            menu.add(0, Utils.REPORT_POST, 0, "Report Flag as inappropriate");
        }/*else{
            menu.add(0, Utils.REMOVE_REPORT_POST, 0, "Revoke Flag report");

        }*/
//        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Flag sel_usr = (Flag)(listView.getItemAtPosition(info.position));
        switch (item.getItemId()) {

            case Utils.DELETE_POST:
                this.deleteFlag(sel_usr);
                return true;

            case Utils.REPORT_POST:
                this.reportFlag(sel_usr);
                return true;
           /* case Utils.REMOVE_REPORT_POST:
                ArrayList<String> delReports =sel_usr.getReports();
                delReports.remove(FacebookUtils.getInstance().getCurrentUserId());
                sel_usr.put("reports", delReports);
                sel_usr.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(getActivity(), "Flag report revoked", Toast.LENGTH_SHORT).show();
                        Utils.mainActivity.refresh();
                    }
                });
                return true;*/
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Deletes the Flag
     * @param f flag to delete
     */
    private void deleteFlag(Flag f){
        f.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                Toast.makeText(getActivity(), "Flag deleted", Toast.LENGTH_SHORT).show();
                Utils.mainActivity.refresh();
            }
        });
    }

    /**
     * Reports a flag
     * @param f flag to report
     */
    private void reportFlag(final Flag f){

        FlagReport report = FlagReport.createFlagReportFromFlag(f);
        report.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.d(TAG, e.getMessage());
                } else {
                    Toast.makeText(getActivity(), "Flag Reported", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }



    private void loadDefaultSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int range = prefs.getInt("seekBar", 1) + 1;
        Utils.MAP_RADIUS = range / 10f;
        Log.d(TAG, "Updated map radius to " + Utils.MAP_RADIUS);
        int step = Utils.stepValues[prefs.getInt("maxFetch", 1)];
        Utils.MAX_PINS = step;
        Log.d(TAG, "Updated max pins to " + Utils.MAX_PINS);
        updateHeaderText();
    }

    public void updateHeaderText(){
        textHeader.setText("within " + (int)(Utils.MAP_RADIUS * 1000) + " meters");
    }

    public static void configureListViewWithFlags()
    {
        if(listView != null)
        {
            adapter =
                    new FlagsArrayAdapter
                                    (Utils.mainActivity,
                                    R.layout.flags_list_item,
                                    PlacesApplication.mService.parseObjects,
                                    Utils.mainActivity);

            listView.setAdapter(adapter);
        }
    }
}
