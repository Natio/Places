package com.gcw.sapienza.places.services;

import android.location.Location;

import com.gcw.sapienza.places.models.Flag;

import java.util.Collection;

/**
 * Created by snowblack on 12/23/14.
 */
public interface ILocationUpdater {

    public void setFlagsNearby(Collection<Flag> c);

    public void setMyFlags(Collection<Flag> c);

    public void setHiddenFlags(Collection<Flag> c);

    public void setBagFlags(Collection<Flag> c);
}
