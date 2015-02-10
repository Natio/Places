package com.gcw.sapienza.places.model;

import java.sql.Timestamp;

/**
 * Created by snowblack on 2/10/15.
 */
public class TimedFlag {
    private Flag flag;
    private long timestamp;
    public TimedFlag(Flag flag, long timestamp){
        this.flag = flag;
        this.timestamp = timestamp;
    }
    public Flag getFlag(){
        return flag;
    }
    public long getTimestamp(){
        return timestamp;
    }
}
