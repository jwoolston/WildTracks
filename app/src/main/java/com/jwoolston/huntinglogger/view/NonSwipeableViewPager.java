package com.jwoolston.huntinglogger.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * ViewPager subclass which prevents the user from swiping to show views.
 *
 * @author Jared Woolston (jwoolston@idealcorp.com)
 * @see <a href="http://stackoverflow.com/a/9650884/1259881">http://stackoverflow.com/a/9650884/1259881</a>
 */
public class NonSwipeableViewPager extends ViewPager {

    public NonSwipeableViewPager(Context context) {
        super(context);
    }

    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }
}
