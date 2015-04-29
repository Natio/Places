package com.gcw.sapienza.places.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by Simone on 3/23/2015.
 */
@ParseClassName("Reported_Comments")
public class CommentReport extends ParseObject{

    @SuppressWarnings("unused")
    private static final String TAG = "CommentReport";

    public CommentReport() {
        super();
    }

    /**
     * Creates a comment's report. The reporter is current user
     *
     * @param f the comment to report
     * @return the configured instance
     */
    public static CommentReport createCommentReportFromComment(Comment f) {
        assert ParseUser.getCurrentUser() != null : "Current user MUST not be null";
        CommentReport report = ParseObject.create(CommentReport.class);
        report.setReportedComment(f);
        report.setReportedByUser(ParseUser.getCurrentUser());
        return report;
    }

    private void setReportedByUser(ParseUser user) {
        this.put("reported_by", user);
    }

    private void setReportedComment(Comment f) {
        this.put("reported_comment", f);
    }
}
