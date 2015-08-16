package com.jwoolston.wildtracks.mapping;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jwoolston.wildtracks.R;

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

    private final Circle mUncertaintyCircle;

    private LatLng mLastLocation;
    private float mZoom = -1;
    private float mAccuracy = 0;
    private float mHeading = -1;
    private int mMarkerDiameter;

    public UserLocationCircle(@NonNull Context context, @NonNull GoogleMap map) {
        mLastLocation = new LatLng(0, 0);
        mMap = map;
        mMarker = mMap.addMarker(new MarkerOptions().position(mLastLocation).visible(false).draggable(false).anchor(0.5f, 0.5f));

        mMarkerDiameter = context.getResources().getDimensionPixelSize(R.dimen.user_location_marker_diameter);
        final int outline_stroke = context.getResources().getDimensionPixelSize(R.dimen.user_location_marker_stroke_width);

        mImage = Bitmap.createBitmap(mMarkerDiameter + 2 * outline_stroke, mMarkerDiameter + 2 * outline_stroke, Bitmap.Config.ARGB_8888);
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

        int color = context.getResources().getColor(R.color.user_location_primary_color);
        color = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color));
        mUncertaintyCircle = mMap.addCircle(new CircleOptions().center(mLastLocation)
            .fillColor(color).strokeColor(0x00FFFFFF).strokeWidth(0).visible(false).zIndex(2));
    }

    public LatLng getLocation() {
        return mMarker.getPosition();
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

        mCanvas.drawCircle(mCanvas.getClipBounds().centerX(), mCanvas.getClipBounds().centerY(), mMarkerDiameter / 2.0f, mOutlinePaint);
        mCanvas.drawCircle(mCanvas.getClipBounds().centerX(), mCanvas.getClipBounds().centerY(), mMarkerDiameter / 2.0f, mMainCirclePaint);

        mUncertaintyCircle.setRadius(mAccuracy);

        mMarker.setIcon(getDescriptor());
        mMarker.setPosition(mLastLocation);
        mUncertaintyCircle.setCenter(mLastLocation);
        if (!mMarker.isVisible()) mMarker.setVisible(true);
        if (!mUncertaintyCircle.isVisible()) mUncertaintyCircle.setVisible(true);
    }
}
