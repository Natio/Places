package com.gcw.sapienza.places.models;


import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;


/**
 * Class that encapsulate the Reported_Posts table on Parse.com
 */
@ParseClassName("Reported_Posts")

public class FlagReport extends ParseObject {
    @SuppressWarnings("unused")
    private static final String TAG = "FlagReport";

    public FlagReport(){
        super();
    }

    private void setReportedByUser(ParseUser user){
        this.put("reported_by", user);
    }


    private void setReportedFlag(Flag f){
        this.put("reported_flag", f);
    }


    /**
     * Creates a Flag's report. The reporter is current user
     * @param f the flag to report
     * @return the configured instance
     */
    public static FlagReport createFlagReportFromFlag(Flag f){
        assert ParseUser.getCurrentUser() != null : "Current user MUST not be null";
        FlagReport report = ParseObject.create(FlagReport.class);
        report.setReportedFlag(f);
        report.setReportedByUser(ParseUser.getCurrentUser());
        return report;
    }

}
