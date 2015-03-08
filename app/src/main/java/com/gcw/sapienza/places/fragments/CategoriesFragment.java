package com.gcw.sapienza.places.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;

/**
 * Created by snowblack on 3/8/15.
 */
public class CategoriesFragment extends Fragment{

    @Override

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.categories_screen, container, false);

        RelativeLayout categoriesLayout = (RelativeLayout) view.findViewById(R.id.categoriesLayout);
        RelativeLayout categoriesSettings = (RelativeLayout) view.findViewById(R.id.categoriesSettings);

        getActivity().getFragmentManager().beginTransaction().replace(R.id.categoriesLayout, new FiltersFragment()).commit();
//        ((MainActivity)getActivity()).getFragmentManager().beginTransaction().replace(R.id.categoriesSettings, new Cate()).commit();

        return view;
    }
}
