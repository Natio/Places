package com.gcw.sapienza.places;

import android.location.Location;

import com.parse.ParseObject;

import java.util.List;

/**
 * Created by snowblack on 12/23/14.
 */
public interface ILocationUpdater {

    public void setLocation(Location l);

    public void setPinsNearby(List<ParseObject> l);

}
