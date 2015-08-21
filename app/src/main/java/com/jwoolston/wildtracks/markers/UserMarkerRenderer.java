package com.jwoolston.wildtracks.markers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.android.ui.SquareTextView;
import com.jwoolston.wildtracks.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarkerRenderer extends DefaultClusterRenderer<UserMarker> {

    private static final String TAG = UserMarkerRenderer.class.getSimpleName();

    private static final int[] BUCKETS = {5, 10, 25, 50, 100, 500, 1000};

    private final Context mContext;

    private final int[] mClusterColors;
    private final SparseArray<BitmapDescriptor> mActivityBitmapDescriptors;
    private final SparseArray<BitmapDescriptor> mClusterBitmapDescriptors;

    private final IconGenerator mClusterIconGenerator;
    private final IconGenerator mMarkerIconGenerator;
    private final float mDensity;
    private final ShapeDrawable mClusterBackground;
    private final ImageView mImageView;
    private final ImageView mBackgroundImage;

    public UserMarkerRenderer(Context context, GoogleMap map, ClusterManager<UserMarker> clusterManager) {
        super(context.getApplicationContext(), map, clusterManager);
        mContext = context.getApplicationContext();

        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.colorClusters);
        mClusterColors = new int[typedArray.length()];
        for (int i = 0; i < mClusterColors.length; ++i) {
            mClusterColors[i] = typedArray.getColor(i, 0);
        }
        typedArray.recycle();

        mDensity = context.getResources().getDisplayMetrics().density;
        mClusterBackground = new ShapeDrawable(new OvalShape());

        mClusterIconGenerator = new IconGenerator(context);
        mClusterIconGenerator.setContentView(makeSquareTextView(context));
        mClusterIconGenerator.setTextAppearance(R.style.ClusterIcon_TextAppearance);
        mClusterIconGenerator.setBackground(makeClusterBackground());

        mMarkerIconGenerator = new IconGenerator(context);

        final View layout = LayoutInflater.from(mContext).inflate(R.layout.layout_custom_marker, null);

        final int markerSize = mContext.getResources().getDimensionPixelSize(R.dimen.user_marker_dimension);
        final int activitySize = mContext.getResources().getDimensionPixelSize(R.dimen.user_marker_activity_dimension);
        final int activityOffset = mContext.getResources().getDimensionPixelSize(R.dimen.user_marker_activity_bottom_offset);
        final FrameLayout frame = new FrameLayout(mContext);
        mBackgroundImage = new ImageView(mContext);
        mImageView = new ImageView(mContext);
        mBackgroundImage.setScaleType(ImageView.ScaleType.CENTER);
        mImageView.setScaleType(ImageView.ScaleType.CENTER);
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(markerSize, markerSize, Gravity.CENTER);
        final FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(activitySize, activitySize, Gravity.CENTER);
        centerParams.bottomMargin = activityOffset;
        frame.addView(mBackgroundImage, params);
        frame.addView(mImageView, centerParams);
        mBackgroundImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.user_marker_background_72dp));
        mMarkerIconGenerator.setContentView(frame);
        mMarkerIconGenerator.setBackground(null);

        mActivityBitmapDescriptors = initializeBitmapDescriptors();
        mClusterBitmapDescriptors = new SparseArray<>(BUCKETS.length);
    }

    @Override
    protected void onBeforeClusterItemRendered(UserMarker marker, MarkerOptions markerOptions) {
        // Draw a single user marker.
        markerOptions.title(marker.getName());
        BitmapDescriptor icon = mActivityBitmapDescriptors.get(marker.getIcon());
        if (icon != null) {
            markerOptions.icon(icon);
            markerOptions.anchor(0.5f, 1.0f);
        } else {
            markerOptions.icon(mActivityBitmapDescriptors.get(0));
        }
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<UserMarker> cluster, MarkerOptions markerOptions) {
        // Draw multiple markers.
        // Note: this method runs on the UI thread. Don't spend too much time in here.
        int bucket = getBucket(cluster);
        BitmapDescriptor descriptor = mClusterBitmapDescriptors.get(bucket);
        if (descriptor == null) {
            mClusterBackground.getPaint().setColor(getColor(bucket));
            descriptor = BitmapDescriptorFactory.fromBitmap(mClusterIconGenerator.makeIcon(getClusterText(bucket)));
            mClusterBitmapDescriptors.put(bucket, descriptor);
        }

        markerOptions.icon(descriptor);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }

    private SparseArray<BitmapDescriptor> initializeBitmapDescriptors() {
        final SparseArray<BitmapDescriptor> descriptors = new SparseArray<>(UserMarker.ICON_MAPPING.length);
        for (int i = 0; i < UserMarker.ICON_MAPPING.length; ++i) {
            mImageView.setImageDrawable(mContext.getResources().getDrawable(UserMarker.ICON_MAPPING[i]));
            Bitmap icon = mMarkerIconGenerator.makeIcon();
            descriptors.put(i, BitmapDescriptorFactory.fromBitmap(icon));
        }
        return descriptors;
    }

    private LayerDrawable makeClusterBackground() {
        int strokeWidth = (int) (mDensity * 3);

        ShapeDrawable outline = new ShapeDrawable(new OvalShape());
        outline.getPaint().setColor(0x80ffffff);

        LayerDrawable background = new LayerDrawable(new Drawable[]{outline, mClusterBackground});
        background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);

        return background;
    }

    private SquareTextView makeSquareTextView(Context context) {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int twelveDpi = (int) (12 * mDensity);

        SquareTextView squareTextView = new SquareTextView(context);
        squareTextView.setLayoutParams(layoutParams);
        squareTextView.setId(R.id.text);
        squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);
        return squareTextView;
    }

    private int getColor(int clusterSize) {
        for (int i = 0; i < BUCKETS.length; i++) {
            if (clusterSize == BUCKETS[i])
                return mClusterColors[i];
        }

        return mClusterColors[0];
    }
}
