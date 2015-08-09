package com.jwoolston.huntinglogger.view;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * @author Dimitar Darazhanski
 * @author Jared Woolston (jwoolston@tenkiv.com)
 *
 * @see <a href="http://dimitar.me/how-to-detect-a-user-pantouchdrag-on-android-map-v2/">http://dimitar.me/how-to-detect-a-user-pantouchdrag-on-android-map-v2/</a>
 */
public class TouchableWrapper extends FrameLayout {

    private long mLastTouched = 0;
    private static final long MIN_SCROLL_TIME = 50;
    private OnUserInteractionCompleteListener mOnUserInteractionCompleteListener;

    public TouchableWrapper(Context context) {
        super(context);
    }

    public TouchableWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouched = SystemClock.uptimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                final long now = SystemClock.uptimeMillis();
                if (now - mLastTouched > MIN_SCROLL_TIME) {
                    // Update the map
                    if (mOnUserInteractionCompleteListener != null) mOnUserInteractionCompleteListener.onUpdateMapAfterUserInteraction();
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setOnUserInteractionCompleteListener(OnUserInteractionCompleteListener listener) {
        mOnUserInteractionCompleteListener = listener;
    }

    public interface OnUserInteractionCompleteListener {
        void onUpdateMapAfterUserInteraction();
    }
}
