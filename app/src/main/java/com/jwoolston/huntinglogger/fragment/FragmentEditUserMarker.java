package com.jwoolston.huntinglogger.fragment;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.gms.maps.model.Marker;
import com.jwoolston.huntinglogger.R;
import com.jwoolston.huntinglogger.mapping.MapManager;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class FragmentEditUserMarker extends Fragment implements Toolbar.OnMenuItemClickListener {

    private static final String TAG = FragmentEditUserMarker.class.getSimpleName();

    private MapManager mMapManager;
    private Marker mMarker;

    private Toolbar mToolbar;
    private NavigationView mNavigationView;

    private int mTintColor;

    public FragmentEditUserMarker() {

    }

    public void setMapManager(MapManager manager) {
        mMapManager = manager;
    }

    public void setMarker(Marker marker) {
        mMarker = marker;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.layout_marker_detail, container, false);
        mTintColor = getResources().getColor(android.R.color.holo_purple);
        mToolbar = (Toolbar) view.findViewById(R.id.detail_view_toolbar);

        mToolbar.setTitle("Edit Marker");
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.inflateMenu(R.menu.menu_edit_marker);
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mNavigationView = (NavigationView) view.findViewById(R.id.detail_view_navigation_view);

        applyTintColor();

        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mMapManager.recenterCamera(mMarker.getPosition());
            }
        });
        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_edit_marker_done) {
            getFragmentManager().beginTransaction().remove(this).commit();
        }
        return false;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        //Check if the superclass already created the animation
        Animation anim = super.onCreateAnimation(transit, enter, nextAnim);

        //If not, and an animation is defined, load it now
        if (anim == null && nextAnim != 0) {
            anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        //If there is an animation for this fragment, add a listener.
        if (anim != null) {
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    onAnimationStarted();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    onAnimationEnded();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    onAnimationRepeated();
                }
            });
        } else {
            Log.d(TAG, "No animation, can't add a listener.");
        }

        return anim;
    }

    private void onAnimationStarted() {

    }

    private void onAnimationEnded() {
        mMapManager.recenterCamera(mMarker.getPosition());
    }

    private void onAnimationRepeated() {

    }

    private void applyTintColor() {
        mToolbar.setBackgroundColor(mTintColor);

        ColorStateList textStateList = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
            },
            new int[]{
                mTintColor,
                mTintColor
            }
        );

        mNavigationView.setItemIconTintList(textStateList);
    }
}
