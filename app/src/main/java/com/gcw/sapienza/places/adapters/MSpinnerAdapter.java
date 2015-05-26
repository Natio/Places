package com.gcw.sapienza.places.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.gcw.sapienza.places.R;

import java.util.List;

/**
 * Created by mic_head on 08/01/15.
 */
public class MSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    @SuppressWarnings("unused")
    private static final String TAG = "MSpinnerFragment";

    Context mContext;
    List<String> values;

    public MSpinnerAdapter(Context context, List<String> values) {
        super(context, R.layout.custom_spinner, values);
        this.mContext = context;
        this.values = values;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.custom_spinner_item, null);
        }

        TextView tv = (TextView) v.findViewById(R.id.text);
        ImageView iv = (ImageView) v.findViewById(R.id.category_color);
        ImageView iv2 = (ImageView) v.findViewById(R.id.category_icon);

        /*
        switch (position) {
            case 0:
                tv.setTextColor(mContext.getResources().getColor(R.color.red));
                break;
            case 1:
                tv.setTextColor(mContext.getResources().getColor(R.color.azure));
                break;
            case 2:
                tv.setTextColor(mContext.getResources().getColor(R.color.orange));
                break;
            case 3:
                tv.setTextColor(mContext.getResources().getColor(R.color.blue));
                break;
            case 4:
                tv.setTextColor(mContext.getResources().getColor(R.color.magenta));
        }
        */

        switch (position) {
            case 0:
                // tv.setTextColor(mContext.getResources().getColor(R.color.grey));
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.red));
                iv.setImageResource(R.drawable.flag_red);
                iv2.setImageResource(R.mipmap.www15);
                break;
            case 1:
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.green));
                iv.setImageResource(R.drawable.flag_green);
                // iv2.setImageResource(R.drawable.none);
                iv2.setImageResource(R.drawable.thoughts);
                break;
            case 2:
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
                iv.setImageResource(R.drawable.flag_yellow);
                iv2.setImageResource(R.drawable.smile);
                break;
            case 3:
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
                iv.setImageResource(R.drawable.flag_blue);
                iv2.setImageResource(R.drawable.music);
                break;
            case 4:
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.grey));
                iv.setImageResource(R.drawable.flag_grey);
                iv2.setImageResource(R.drawable.eyes);
                break;
            case 5:
                // iv.setBackgroundColor(mContext.getResources().getColor(R.color.purple));
                iv.setImageResource(R.drawable.flag_purple);
                iv2.setImageResource(R.drawable.food);
                // tv.setTextColor(mContext.getResources().getColor(R.color.black));
        }

        tv.setBackgroundResource(R.drawable.spinner_selector);
        tv.setPadding(0, 15, 0, 15);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(24);
        tv.setText(values.get(position));

        return v;
    }
}
