package com.jwoolston.wildtracks.animation;

import android.support.v4.view.ViewPager;
import android.view.View;

import com.ToxicBakery.viewpager.transforms.ABaseTransformer;

/**
 * {@link ABaseTransformer} implementation for a {@link ViewPager} which animates vertically from the bottom of the screen.
 *
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class SlideInTransformer extends ABaseTransformer {

    @Override
    protected void onTransform(View view, float position) {
        final float h = position * view.getHeight();
        view.setTranslationY(h);
    }
}
