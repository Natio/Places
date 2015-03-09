package com.gcw.sapienza.places.services;

import android.location.Location;

import com.gcw.sapienza.places.models.Flag;

import java.util.List;

/**
 * Created by snowblack on 12/23/14.
 */
public interface ILocationUpdater {

    public void setLocation(Location l);

    public void setFlagsNearby(List<Flag> l);

    public void setMyFlags(List<Flag> myFlags);

    public void setHiddenFlags(List<Flag> hiddenFlags);
}
