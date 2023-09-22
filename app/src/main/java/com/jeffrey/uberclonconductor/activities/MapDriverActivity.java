package com.jeffrey.uberclonconductor.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jeffrey.uberclonconductor.R;
import com.jeffrey.uberclonconductor.includes.MyToolbar;
import com.jeffrey.uberclonconductor.providers.AuthProvider;
import com.jeffrey.uberclonconductor.providers.GeofireProvider;
import com.jeffrey.uberclonconductor.providers.TokenProvider;
import com.jeffrey.uberclonconductor.services.ForegroundService;
import com.jeffrey.uberclonconductor.utils.CarMoveAnim;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private TokenProvider mTokenProvider;


    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private Marker mMarker;

    private Button mButtonConnect;
    private boolean mIsConnect = false;

    private LatLng mCurrentLatLng;

    private ValueEventListener mListener;

    private boolean mExtraConnect;

    SharedPreferences mPref;

    private GoogleApiClient mGoogleApiClient;
    private final int REQUEST_CHECK_SETTINGS = 0x1;
    private boolean mIsStartLocation = false;
    LatLng mStartLatLng;
    LatLng mEndLatLng;
    LocationManager mLocationManager;


    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (mStartLatLng != null) {
                mEndLatLng = mStartLatLng;
            }

            mStartLatLng = new LatLng(mCurrentLatLng.latitude, mCurrentLatLng.longitude);

            if (mEndLatLng != null) {
                CarMoveAnim.carAnim(mMarker, mEndLatLng, mStartLatLng);
            }

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(18f)
                            .build()
            ));

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


    LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    //YA RECONOCIO LA UBICACION POR PRIMERA VEZ
                    if (!mIsStartLocation) {
                        mMap.clear();
                        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mIsStartLocation = true;
                        mMarker = mMap.addMarker(new MarkerOptions().position(
                                new LatLng(location.getLatitude(), location.getLongitude())
                                )
                                        .title("Tu posicion")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_location_driver))
                        );
                        // OBTENER LA LOCALIZACION DEL USUARIO EN TIEMPO REAL
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                        .zoom(16f)
                                        .build()
                        ));
                        updateLocation();

                        if (ActivityCompat.checkSelfPermission(MapDriverActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapDriverActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                2000, 10, locationListenerGPS);
                        stopLocation();
                        //Log.d("ENTRO", "ACTUALIZANDO PSOICIN");
                    }

                    /*if (mMarker != null) {
                        mMarker.remove();
                    }*/

                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);
        MyToolbar.show(this, "Conductor", false);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();

        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mExtraConnect = getIntent().getBooleanExtra("connect", false);

        mButtonConnect = findViewById(R.id.btnConnect);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsConnect) {
                    disconnect();
                } else {
                    startLocation();
                }
            }
        });

        mGoogleApiClient = getAPIClientInstance();
        if(mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }


        mPref = getApplicationContext().getSharedPreferences("RideStatus", MODE_PRIVATE);
        String status = mPref.getString("status","");
        String idClient = mPref.getString("idClient","");

        if(status.equals("start") || status.equals("rude")){
            goToMapDriverActivity(idClient);
        }
        else{
            generateToken();
            deleteDriverWorking();
        }
    }

    private void goToMapDriverActivity(String idClient) {
        Intent intent = new Intent(MapDriverActivity.this, MapDriverBookingActivity.class);
        intent.putExtra("idClient", idClient);
        startActivity(intent);
    }

    private void deleteDriverWorking() {
        mGeofireProvider.deleteDriverWorking(mAuthProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isDriverWorking();

                if(mExtraConnect){
                    startLocation();
                }
            }
        });
    }

    private void checkIfDriverIsActived() {
        mGeofireProvider.getDriver(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    startLocation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void stopLocation() {
        if (mLocationCallback != null && mFusedLocation != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocation();
        removeLocation();
        /*if (mListener != null && mFusedLocation != null) {
            mGeofireProvider.isDriverWorking(mAuthProvider.getId()).removeEventListener(mListener);
        }*/
        if(mListener != null){
            if(mAuthProvider.existSession()){
                mGeofireProvider.isDriverWorking(mAuthProvider.getId()).removeEventListener(mListener);
            }
        }
    }

    private void isDriverWorking() {
        mListener = mGeofireProvider.isDriverWorking(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    disconnect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateLocation() {
        if (mAuthProvider.existSession() && mCurrentLatLng != null) {
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
        }
    }

    private GoogleApiClient getAPIClientInstance() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).build();
        return  googleApiClient;
    }

    private void requestGPSSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        final PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();

                if (status.getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                    Toast.makeText(MapDriverActivity.this, "El GPS ya esta activado", Toast.LENGTH_SHORT).show();
                }
                else if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(MapDriverActivity.this, REQUEST_CHECK_SETTINGS);
                        if (ActivityCompat.checkSelfPermission(MapDriverActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapDriverActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(false);
                    } catch (IntentSender.SendIntentException e) {
                        Toast.makeText(MapDriverActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
                else if (status.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    Toast.makeText(MapDriverActivity.this, "La configuracion del GPS tiene algun error o esta disponible", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(false);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

        checkIfDriverIsActived();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (gpsActived()) {
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    } else {
                        //showAlertDialogNOGPS();
                        requestGPSSettings();
                    }
                } else {
                    checkLocationPermissions();
                }
            } else {
                checkLocationPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
        else {
            //showAlertDialogNOGPS();
            requestGPSSettings();
        }
    }

    private void showAlertDialogNOGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicacion para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                }).create().show();
    }

    private boolean gpsActived() {
        boolean isActive = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isActive = true;
        }
        return isActive;
    }

    private void disconnect() {

        removeLocation();
        mButtonConnect.setText("Conectarse");
        mIsConnect = false;
        mIsStartLocation = false;
        //mFusedLocation.removeLocationUpdates(mLocationCallback); XXX
        mGeofireProvider.removeLocation(mAuthProvider.getId());

        /*if (mFusedLocation != null) {
            mButtonConnect.setText("Conectarse");
            mIsConnect = false;
            mFusedLocation.removeLocationUpdates(mLocationCallback);
            if (mAuthProvider.existSession()) {
                mGeofireProvider.removeLocation(mAuthProvider.getId());
            }
        }
        else {
            Toast.makeText(this, "No te puedes desconectar", Toast.LENGTH_SHORT).show();
        }*/
    }

    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
                    mButtonConnect.setText("Desconectarse");
                    mIsConnect = true;
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                }
                else {
                    //showAlertDialogNOGPS();
                    requestGPSSettings();
                }
            }
            else {
                checkLocationPermissions();
            }
        } else {
            if (gpsActived()) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            }
            else {
                //showAlertDialogNOGPS();
                requestGPSSettings();
            }
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicacion requiere de los permisos de ubicacion para poder utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            }
            else {
                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
       /* if (item.getItemId() == R.id.action_logout) {
            logout();
        }*/
        if (item.getItemId() == R.id.action_update) {
            Intent intent = new Intent(MapDriverActivity.this, UpdateProfileDriverActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_history) {
            Intent intent = new Intent(MapDriverActivity.this, HistoryBookingDriverActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    void logout() {
        disconnect();
        mAuthProvider.logout();
        Intent intent = new Intent(MapDriverActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    void generateToken() {
        mTokenProvider.create(mAuthProvider.getId());
    }

    private void removeLocation() {
        if (locationListenerGPS != null) {
            mLocationManager.removeUpdates(locationListenerGPS);
        }
    }

}
