package com.gcw.sapienza.places.services;

import android.location.Location;

import com.gcw.sapienza.places.models.Flag;

import java.util.HashMap;
import java.util.List;

/**
 * Created by snowblack on 12/23/14.
 */
public interface ILocationUpdater {

    public void setLocation(Location l);

    public void setFlagsNearby(HashMap<String, Flag> l);

    public void setMyFlags(HashMap<String, Flag> myFlags);

    public void setHiddenFlags(HashMap<String, Flag> hiddenFlags);

    public void setBagFlags(HashMap<String, Flag> bagFlags);
}
