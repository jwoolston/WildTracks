package com.jwoolston.huntinglogger.mapping;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jwoolston.huntinglogger.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserLocationCircle {

    private final GoogleMap mMap;
    private final Marker mMarker;
    private final Bitmap mImage;
    private final Canvas mCanvas;

    private final Paint mMainCirclePaint;
    private final Paint mOutlinePaint;
    private final Paint mUncertaintyPaint;

    private final RectF mMainCircleBounds;

    private LatLng mLastLocation;
    private float mZoom = -1;
    private float mAccuracy = 0;
    private float mHeading = -1;

    public UserLocationCircle(@NonNull Context context, @NonNull GoogleMap map) {
        mMap = map;
        mMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).visible(false).draggable(false));

        final int diameter = context.getResources().getDimensionPixelSize(R.dimen.user_location_max_marker_diameter);
        final int circle_diameter = context.getResources().getDimensionPixelSize(R.dimen.user_location_marker_diameter);
        final int outline_stroke = context.getResources().getDimensionPixelSize(R.dimen.user_location_marker_stroke_width);
        final float center = diameter / 2.0f;

        mImage = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mImage);

        mMainCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainCirclePaint.setStyle(Paint.Style.FILL);
        mMainCirclePaint.setColor(context.getResources().getColor(R.color.user_location_primary_color));

        mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setColor(context.getResources().getColor(R.color.user_location_stroke_color));
        mOutlinePaint.setStrokeWidth(outline_stroke);
        //mOutlinePaint.setShadowLayer(circle_diameter, outline_stroke, outline_stroke, Color.BLACK);

        mUncertaintyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUncertaintyPaint.setStyle(Paint.Style.FILL);
        mUncertaintyPaint.setColor(context.getResources().getColor(R.color.user_location_primary_color));
        mUncertaintyPaint.setAlpha(128);

        mMainCircleBounds = new RectF(
            center - (circle_diameter / 2.0f),
            center - (circle_diameter / 2.0f),
            center + (circle_diameter / 2.0f),
            center + (circle_diameter / 2.0f)
        );
    }

    public void onLocationUpdate(@NonNull LatLng location) {
        mLastLocation = location;
        redraw();
    }

    public void onLocationUpdate(@NonNull Location location) {
        mLastLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mAccuracy = location.getAccuracy();
        mHeading = location.getBearing();
        redraw();
    }

    public void onCameraUpdate(float zoom) {
        mZoom = zoom;
        redraw();
    }

    private BitmapDescriptor getDescriptor() {
        return BitmapDescriptorFactory.fromBitmap(mImage);
    }

    private void redraw() {
        if (mLastLocation == null) return;
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (mZoom >= 0) {
            // Draw the uncertainty circle
            final double meters_per_pixel = MapManager.metersPerPixel(mLastLocation.latitude, mZoom);
            final float radius = (float) (mAccuracy / meters_per_pixel);
            mCanvas.drawCircle(mMainCircleBounds.centerX(), mMainCircleBounds.centerY(), radius, mUncertaintyPaint);
        }

        mCanvas.drawCircle(mMainCircleBounds.centerX(), mMainCircleBounds.centerY(), mMainCircleBounds.width() / 2.0f, mOutlinePaint);
        mCanvas.drawCircle(mMainCircleBounds.centerX(), mMainCircleBounds.centerY(), mMainCircleBounds.width() / 2.0f, mMainCirclePaint);

        mMarker.setIcon(getDescriptor());
        mMarker.setPosition(mLastLocation);
        if (!mMarker.isVisible()) mMarker.setVisible(true);
    }
}
