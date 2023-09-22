package com.jeffrey.uberclonconductor.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.jeffrey.uberclonconductor.R;
import com.jeffrey.uberclonconductor.providers.AuthProvider;
import com.jeffrey.uberclonconductor.providers.GeofireProvider;

public class ForegroundService extends Service {
    public final String CHANNEL_ID = "com.optic.ubercloneconductor";

    Handler handler = new Handler();
    LatLng mCurrentLatLng;
    GeofireProvider mGeofireProvider;
    AuthProvider mAuthProvider;
    LocationRequest mLocationRequest;
    LocationManager mLocationManager;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.d("SERVICIO", "Temporizador prueba");
            handler.postDelayed(runnable, 1000);
        }
    };

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            updateLocation();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void startLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListenerGPS);
    }

    private void updateLocation() {
        if (mAuthProvider.existSession() && mCurrentLatLng != null) {
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //handler.postDelayed(runnable, 1000);
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("drivers_working");
        startLocation();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_car)
                .setContentTitle("Viaje en curso")
                .setContentText("App corriendo en segundo plano")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyForegroundService();
        } else {
            startForeground(50, notification);
        }

        return START_STICKY;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyForegroundService() {
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_car)
                .setContentTitle("Viaje en curso")
                .setContentText("App corriendo en segundo plano")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(50, notification);
    }

    private void stopLocation() {
        if (locationListenerGPS != null) {
            mLocationManager.removeUpdates(locationListenerGPS);
        }
    }

    @Override
    public void onDestroy() {
        stopLocation();
        super.onDestroy();
    }

}
