package com.gcw.sapienza.places.layouts;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;

public class MSwipeRefreshLayout extends SwipeRefreshLayout
{

    public MSwipeRefreshLayout(Context context)
    {
        super(context, null);
    }

    public MSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private OnChildScrollUpListener mScrollListenerNeeded;

    public static interface OnChildScrollUpListener {
        public boolean canChildScrollUp();
    }

    public void setOnChildScrollUpListener(OnChildScrollUpListener listener) {
        mScrollListenerNeeded = listener;
    }

    @Override
    public boolean canChildScrollUp() {
        if (mScrollListenerNeeded == null) {
            Log.e(MSwipeRefreshLayout.class.getSimpleName(), "OnChildScrollListener is null!");
        }
        return mScrollListenerNeeded == null ? false : mScrollListenerNeeded.canChildScrollUp();
    }
}