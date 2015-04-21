package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Class that helps storing flags.
 *
 * It is very easy to add a new flag type, just add the relative enum in Type
 *
 * Created by paolo on 26/03/15.
 */



public final class FlagsStorage {

    private static final String TAG = "FlagsStorage";


    private final EnumMap<Type, ArrayList<Flag>> storage;

    public enum Type{
        NEARBY, // nearby flags
        MY, //my flags
        HIDDEN, //hidden flags
        BAG// bag flags
    }


    private static final FlagsStorage shared_instance = new FlagsStorage();

    private FlagsStorage(){
        this.storage = new EnumMap<>(Type.class);
    }

    /**
     * Returns the shared instance of FlagsStorage
     * @return the shared instance
     */
    public static FlagsStorage getSharedStorage(){
        return shared_instance;
    }


    /**
     * Stores flags. If flags of a type where already stored, storing other flags of the same type will overwrite them.
     * @param flags list of flags to add
     * @param type type of the flag
     */
    public synchronized void storeFlagsWithType(Collection<Flag> flags, Type type){
        this.storage.put(type, new ArrayList<>(flags));
    }

    /**
     * Returns the list of flags associated of a type. If there are no flags for a given type an epty list is returned
     * @param type type of the list to return
     * @return Returns the list of flags associated of a type. If there are no flags for a given type an epty list is returned
     */
    public synchronized List<Flag> fetchFlagsWithType(Type type){
        ArrayList<Flag> value = this.storage.get(type);
        if(value == null){
            return new ArrayList<>();
        }
        return new ArrayList<>(value);
    }


    /**
     * Returns a list of flag associated to a given type, sorted according to preferences
     * @param context context for preferences
     * @param type type of the flag
     * @return a list of flag associated to a given type, sorted according to preferences
     */
    public List<Flag> getOrderedFlags(Context context, Type type) {

        List<Flag> flags = this.fetchFlagsWithType(type);

        SharedPreferences preferences;

        try {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }catch (NullPointerException e){
            Log.e(TAG, e.getMessage());
            PlacesUtils.showToast(context, "There was a problem retrieving Flags data", Toast.LENGTH_SHORT);
            return flags;
        }

        boolean archaeologist = preferences.getBoolean("timeFilter", false);

        //we make the 'archeologist' setting work also for MyFlag and Bag pages
        if(archaeologist){
            Collections.sort(flags, new FlagComparator(true));
        }else{
            Collections.sort(flags, new FlagComparator(false));
        }
        return flags;
    }


}
