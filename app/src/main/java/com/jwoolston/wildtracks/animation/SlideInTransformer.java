package com.jwoolston.wildtracks.animation;

import android.view.View;

import com.ToxicBakery.viewpager.transforms.ABaseTransformer;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class SlideInTransformer extends ABaseTransformer {
    @Override
    protected void onTransform(View view, float position) {
        final float h = position * view.getHeight();
        view.setTranslationY(h);
    }
}
