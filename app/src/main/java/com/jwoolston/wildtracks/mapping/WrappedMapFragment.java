package com.jwoolston.wildtracks.mapping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.SupportMapFragment;
import com.jwoolston.wildtracks.view.TouchableWrapper;

/**
 * @author Dimitar Darazhanski
 * @see <a href="http://dimitar.me/how-to-detect-a-user-pantouchdrag-on-android-map-v2/">http://dimitar.me/how-to-detect-a-user-pantouchdrag-on-android-map-v2/</a>
 */
public class WrappedMapFragment extends SupportMapFragment {

    public View mOriginalContentView;
    public TouchableWrapper mTouchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
        mTouchView = new TouchableWrapper(getActivity());
        mTouchView.addView(mOriginalContentView);
        return mTouchView;
    }

    @Override
    public View getView() {
        return mOriginalContentView;
    }

    public void setOnUserInteractionCompleteListener(TouchableWrapper.OnUserInteractionCompleteListener listener) {
        mTouchView.setOnUserInteractionCompleteListener(listener);
    }
}
