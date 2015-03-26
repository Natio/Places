package com.gcw.sapienza.places.utils;

/**
 * Created by snowblack on 3/3/15.
 */
public enum Category {
    NONE, THOUGHTS, FUN, MUSIC, LANDSCAPE, FOOD;

    @Override
    public String toString() {
        switch (this){
            case THOUGHTS:
                return "Thoughts";
            case FUN:
                return "Fun";
            case MUSIC:
                return "Music";
            case LANDSCAPE:
                return "Landscape";
            case FOOD:
                return "Food";
            default:
                return "None";
        }

    }
}
