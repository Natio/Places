package com.gcw.sapienza.places.models.manager;

import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by paolo on 31/03/15.
 */
public class Promise<K extends ParseObject> {
    private static final String TAG = "Promise";
    ParseQuery<K> query;
    private ModelCallback<K> modelCallback;
    private ErrorCallback errorCallback;
    private Comparator<K> comparator;///to compare results


    protected Promise(ParseQuery<K> query){
        this.query = query;
    }

    /**
     * Sets desired cache policy
     * @param cachePolicy cache policy to use
     * @return current promise
     */
    public Promise<K> cache(ParseQuery.CachePolicy cachePolicy){
        this.query.setCachePolicy(cachePolicy);
        return this;
    }


    /**
     * Sets error callback
     * @param errorCallback callback in case of error
     * @return current promise
     */
    public Promise<K> error(ErrorCallback errorCallback){
        this.errorCallback = errorCallback;
        return this;
    }

    /**
     * Sorts results on client side. In order to reduce loading time from Parse.com
     * @param comparator used to compare the objects
     * @return current promise
     */
    public Promise<K> sort(Comparator<K> comparator){
        this.comparator = comparator;
        return this;
    }


    /**
     *
     * @param cbk the callback in case of success
     * @return current promise
     */
    public Promise<K> success(ModelCallback<K> cbk){
        this.modelCallback = cbk;
        return this;
    }

    /**
     * Starts the promise
     */
    public void start(){
        this.query.findInBackground(new FindCallback<K>() {
            @Override
            public void done(List<K> ks, ParseException e) {
                Promise.this.onFinish(ks, e);
            }
        });
    }

    /**
     * Called when data is ready
     * @param ks results list
     * @param e error
     */
    private void onFinish(List<K> ks, ParseException e){
        if(e != null){
            Log.d(TAG, e.getMessage(), e);
            if(this.errorCallback != null){
                this.errorCallback.error(e);
            }
        }
        else if(this.modelCallback != null){


            if(this.comparator == null){
                this.modelCallback.result(ks);
            }
            else{
                Collections.sort(ks, Promise.this.comparator);
                this.modelCallback.result(ks);
            }

        }
    }


}
