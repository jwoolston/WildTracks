package com.jwoolston.wildtracks.markers;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarker implements ClusterItem {

    private static final String TAG = UserMarker.class.getSimpleName();

    private Marker mMarker;
    private MarkerOptions mMarkerOptions;
    
    private LatLng mLocation;
    private long mId = -1;
    private String mName = "";
    private long mCreated = -1;
    private int mActivity = 0;
    private int mType = 0;
    private String mNotes = "";

    public UserMarker(MarkerOptions options) {
        mMarkerOptions = options;
    }

    public UserMarker(LatLng position) {
        mLocation = new LatLng(position.latitude, position.longitude);
    }

    public UserMarker(int id, String name, LatLng position, long created, int activity, int type, String notes) {
        mId = id;
        mName = name;
        mMarkerOptions = new MarkerOptions();
        mMarkerOptions.position(position);
        mLocation = new LatLng(position.latitude, position.longitude);
        mCreated = created;
        mActivity = activity;
        mType = type;
        mNotes = notes;
    }

    public void addToMap(GoogleMap map) {
        mMarker = map.addMarker(mMarkerOptions);
    }

    public void removeFromMap() {
        if (mMarker != null) {
            Log.d(TAG, "Removing temporary map marker.");
            // Remove from the map
            mMarker.remove();
            // Clear the params for handling the marker ourself
            mMarker = null;
            mMarkerOptions = null;
        }
    }

    public Marker getMarker() {
        return mMarker;
    }

    public void setPosition(LatLng position) {
        mLocation = new LatLng(position.latitude, position.longitude);
        if (mMarker != null) {
            mMarker.setPosition(position);
        } else if (mMarkerOptions != null) {
            mMarkerOptions.position(position);
        }
    }

    @Override
    public LatLng getPosition() {
        return mLocation;
    }

    public void setId(long id) {
        mId = id;
    }

    public void setName(String name) {
        mName = name;
        if (mMarker != null) mMarker.setTitle(mName);
    }

    public void setCreated(long created) {
        mCreated = created;
    }

    public void setActivity(int activity) {
        mActivity = activity;
    }

    public void setType(int type) {
        mType = type;
    }

    public void setNotes(String notes) {
        mNotes = notes;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public double getLatitude() {
        return mLocation.latitude;
    }

    public double getLongitude() {
        return mLocation.longitude;
    }

    public long getCreated() {
        return mCreated;
    }

    public int getActivity() {
        return mActivity;
    }

    public int getType() {
        return mType;
    }

    public String getNotes() {
        return mNotes;
    }

    @Override
    public String toString() {
        return "UserMarker{" +
            "mName='" + mName + '\'' +
            ", mCreated=" + mCreated +
            ", mActivity=" + mActivity +
            ", mType=" + mType +
            ", mNotes='" + mNotes + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserMarker marker = (UserMarker) o;

        return mId == marker.mId;

    }

    @Override
    public int hashCode() {
        return (int) (mId & 0xFFFF);
    }
}
