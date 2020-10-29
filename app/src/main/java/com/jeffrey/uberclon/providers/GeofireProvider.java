package com.jeffrey.uberclon.providers;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GeofireProvider {

    private DatabaseReference mDatabase;
    private GeoFire mGeofire;

    public GeofireProvider () {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("active_drivers");
        mGeofire = new GeoFire(mDatabase);
    }

    public void saveLocation(String idDriver, LatLng latLng) {
        mGeofire.setLocation(idDriver, new GeoLocation(latLng.latitude, latLng.longitude));
    }

    public void removeLocation(String idDriver) {
        mGeofire.removeLocation(idDriver);
    }

    public GeoQuery getActiveDrivers(LatLng latLng) {
        GeoQuery geoQuery = mGeofire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), 10);
        geoQuery.removeAllListeners();
        return geoQuery;
    }

}
