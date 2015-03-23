package com.gcw.sapienza.places.models;

import java.util.Comparator;

/**
 * Created by snowblack on 3/23/15.
 */
public class FlagComparator implements Comparator<Flag>{

    private boolean isReversed;

    public FlagComparator(){
    }

    public FlagComparator(boolean reverse){
        isReversed = reverse;
    }

    @Override
    public int compare(Flag firstFlag, Flag secondFlag) {
        if(firstFlag.getDate().after(secondFlag.getDate())){
            return isReversed ? 1 : -1;
        }
        else{
            return isReversed ? -1 : 1;
        }
    }
}
