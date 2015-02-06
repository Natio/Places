package com.gcw.sapienza.places.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.gcw.sapienza.places.R;

import java.util.List;

/**
 * Created by mic_head on 08/01/15.
 */
public class MSpinnerAdapter extends ArrayAdapter implements SpinnerAdapter{
    @SuppressWarnings("unused")
    private static final String TAG = "MSpinnerFragment";

    Context mContext;
    List<String> values;

    public MSpinnerAdapter(Context context, List<String> values)
    {
        super(context, R.layout.custom_spinner,values);
        this.mContext = context;
        this.values = values;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){

        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(android.R.layout.simple_spinner_dropdown_item, null);
        }

        TextView tv=(TextView) v.findViewById(android.R.id.text1);

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

        switch (position)
        {
            case 0:
                tv.setTextColor(mContext.getResources().getColor(R.color.grey));
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                tv.setTextColor(mContext.getResources().getColor(R.color.black));
        }

        tv.setBackgroundResource(R.drawable.spinner_selector);
        tv.setPadding(0, 15, 0, 15);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(22);
        tv.setText(values.get(position));

        return v;
    }
}
