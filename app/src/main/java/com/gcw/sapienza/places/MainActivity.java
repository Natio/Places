package com.gcw.sapienza.places;

import android.app.IntentService;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;



public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent locInt = new Intent(this, LocationService.class);
        Log.d("Main Activity", "Starting Location Service");
        stopService(locInt);
        startService(locInt);

        setContentView(R.layout.activity_main);

        //button listener for showing the list
        Button btn_list = (Button)this.findViewById(R.id.button_list);
        btn_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open the list of flags
                Intent show_list_intent = new Intent(MainActivity.this, FlagsListActivity.class);
                MainActivity.this.startActivity(show_list_intent);
            }
        });

        //button listener for showing the map
        Button btn_map = (Button)this.findViewById(R.id.button_map);
        btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open the list of flags
                Intent show_map_intent = new Intent(MainActivity.this, FlagsMapActivity.class);
                MainActivity.this.startActivity(show_map_intent);
            }
        });

        Button btn_share = (Button)this.findViewById(R.id.button_share);
        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent show_share = new Intent(MainActivity.this, ShareActivity.class);
                MainActivity.this.startActivity(show_share);
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
