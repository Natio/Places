package com.gcw.sapienza.places.models.manager;

import com.parse.ParseObject;

import java.util.List;

/**
 * Created by paolo on 31/03/15.
 */
public interface ModelCallback<T extends ParseObject> {
    public void result(List<T> object);
}
