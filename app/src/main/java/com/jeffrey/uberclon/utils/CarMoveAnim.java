package com.jeffrey.uberclon.utils;

import android.animation.ValueAnimator;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class CarMoveAnim {
    static void carAnim(final Marker carMarker, final GoogleMap googleMap, final LatLng startPosition,
                        final LatLng endPosition, int duration, final GoogleMap.CancelableCallback callback) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        if (duration == 0 || duration < 2000) duration = 2000;
        valueAnimator.setDuration(duration);
        final LatLngInterpolatorNew latLngInterpolator = new LatLngInterpolatorNew.LinearFixed();
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float v = valueAnimator.getAnimatedFraction();
                double lng = v * endPosition.longitude + (1 - v)
                        * startPosition.longitude;
                double lat = v * endPosition.latitude + (1 - v)
                        * startPosition.latitude;
                LatLng newPos = latLngInterpolator.interpolate(v, startPosition, endPosition);
                carMarker.setPosition(newPos);
                carMarker.setAnchor(0.5f, 0.5f);
                carMarker.setRotation((float) bearingBetweenLocations(startPosition, endPosition));
                if (callback != null) {
                    googleMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition
                                    (new CameraPosition.Builder()
                                            .target(newPos)
                                            .bearing((float) bearingBetweenLocations(startPosition, endPosition))
                                            .zoom(12)
                                            .build()), callback);
                } else {
                    googleMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition
                                    (new CameraPosition.Builder()
                                            .target(newPos)
                                            .bearing((float) bearingBetweenLocations(startPosition, endPosition))
                                            .zoom(12)
                                            .build()));
                }
            }
        });
        valueAnimator.start();
    }

    public static void carAnim(final Marker marker, final LatLng start, final LatLng end) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(2000);
        final LatLngInterpolatorNew latLngInterpolator = new LatLngInterpolatorNew.LinearFixed();
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float v = valueAnimator.getAnimatedFraction();
                LatLng newPos = latLngInterpolator.interpolate(v, start, end);
                Log.d("ENTRO", "Lat: " + newPos.latitude + " Lng " + newPos.longitude);

                marker.setPosition(newPos);
                marker.setAnchor(0.5f, 0.5f);
                marker.setRotation((float) bearingBetweenLocations(start, end));
            }
        });
        valueAnimator.start();
    }

    private static double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;
        double dLon = (long2 - long1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    public interface LatLngInterpolatorNew {
        LatLng interpolate(float fraction, LatLng a, LatLng b);

        class LinearFixed implements LatLngInterpolatorNew {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lngDelta = b.longitude - a.longitude;
                if (Math.abs(lngDelta) > 180) lngDelta -= Math.signum(lngDelta) * 360;
                double lng = lngDelta * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }
    }
}
