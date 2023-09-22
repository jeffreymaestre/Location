package com.jeffrey.uberclon.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jeffrey.uberclon.R;
import com.jeffrey.uberclon.models.ClientBooking;
import com.jeffrey.uberclon.models.FCMBody;
import com.jeffrey.uberclon.models.FCMResponse;
import com.jeffrey.uberclon.providers.AuthProvider;
import com.jeffrey.uberclon.providers.ClientBookingProvider;
import com.jeffrey.uberclon.providers.GeofireProvider;
import com.jeffrey.uberclon.providers.GoogleApiProvider;
import com.jeffrey.uberclon.providers.NotificationProvider;
import com.jeffrey.uberclon.providers.TokenProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDriverActivity extends AppCompatActivity {
    private LottieAnimationView mAnimation;
    private TextView mTextViewLookingFor;
    private Button mButtonCancelRequest;
    private GeofireProvider mGeofireProvider;

    private String mExtraOrigin;
    private String mExtraDestination;
    private double mExtraOriginLat;
    private double mExtraOriginLng;
    private double mExtraDestinationLat;
    private double mExtraDestinationLng;
    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private double mRadius = 0.1;
    private boolean mDriverFound = false;
    private String  mIdDriverFound = "";
    private LatLng mDriverFoundLatLng;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;
    private GoogleApiProvider mGoogleApiProvider;

    private ValueEventListener mListener;

    private ArrayList<String> mDriversNotAccept = new ArrayList<>();

    private int mTimeLimit = 0;
    private Handler mHandler = new Handler();
    private boolean mIsFinishSearch = false;
    private boolean mIsLookingFor = false;

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTimeLimit < 35) {
                mTimeLimit++;
                mHandler.postDelayed(mRunnable, 1000);
            }
            else {
                if (mIdDriverFound != null) {
                    if (!mIdDriverFound.equals("")) {
                        mClientBookingProvider.updateStatus(mAuthProvider.getId(), "cancel");
                        restartRequest();
                    }
                }
                mHandler.removeCallbacks(mRunnable);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        mAnimation = findViewById(R.id.animation);
        mTextViewLookingFor = findViewById(R.id.textViewLookingFor);
        mButtonCancelRequest = findViewById(R.id.btnCancelRequest);

        mAnimation.playAnimation();

        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraOriginLat = getIntent().getDoubleExtra("origin_lat", 0);
        mExtraOriginLng = getIntent().getDoubleExtra("origin_lng", 0);
        mExtraDestinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        mExtraDestinationLng = getIntent().getDoubleExtra("destination_lng", 0);
        mOriginLatLng = new LatLng(mExtraOriginLat, mExtraOriginLng);
        mDestinationLatLng= new LatLng(mExtraDestinationLat, mExtraDestinationLng);

        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();
        mNotificationProvider = new NotificationProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mAuthProvider = new AuthProvider();
        mGoogleApiProvider = new GoogleApiProvider(RequestDriverActivity.this);

        mButtonCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRequest();
            }
        });

        getClosestDriver();
    }

    private void cancelRequest() {

        mClientBookingProvider.delete(mAuthProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendNotificationCancel();
            }
        });

    }

    /**
     * RETORNAR SI EL ID DEL CODNDUCTOR ENCONTRADO YA CANCELO EL VIAJE
     * @param idDriver
     * @return
     */
    private boolean isDriverCancel(String idDriver) {
        for (String id: mDriversNotAccept) {
            if (id.equals(idDriver)) {
                return true;
            }
        }
        return false;
    }

    private void checkStatusClientBooking() {
        mListener = mClientBookingProvider.getStatus(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    String status = dataSnapshot.getValue().toString();
                    if (status.equals("accept")) {
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientBookingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else if (status.equals("cancel")) {

                        if (mIsLookingFor) {
                            restartRequest();
                        }

                        Toast.makeText(RequestDriverActivity.this, "El conductor no acepto el viaje", Toast.LENGTH_SHORT).show();
                        /*
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();

                         */
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void restartRequest() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mTimeLimit = 0;
        mIsLookingFor = false;
        mDriversNotAccept.add(mIdDriverFound);
        mDriverFound = false;
        mIdDriverFound = "";
        mRadius = 0.1f;
        mIsFinishSearch = false;
        mTextViewLookingFor.setText("BUSCANDO CONDUCTOR");

        getClosestDriver();
    }

    private void getClosestDriver() {
        mGeofireProvider.getActiveDrivers(mOriginLatLng, mRadius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                // INGRESA CUANDO ENCUENTRA UN CONDUCTOR EN UN RADIO DE BUSQUEDA
                if (!mDriverFound && !isDriverCancel(key) && !mIsFinishSearch) {

                    // ESTA BUSCANDO UN CONDUCTOR QUE AUN NO RECHAZADO LA SOLICTUD
                    mIsLookingFor = true;
                    mClientBookingProvider.getClientBookingByDriver(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isDriverNotification = false;

                            if(!mIsFinishSearch) {
                                for (DataSnapshot d: snapshot.getChildren()) {

                                    if (d.exists()) {
                                        if (d.hasChild("status")) {
                                            String status = d.child("status").getValue().toString();
                                            if (status.equals("create") || status.equals("accept")) {
                                                isDriverNotification = true;
                                                break;
                                            }

                                        }
                                        else {
                                            Log.d("STATUS", "No existe el estado Estado child");
                                        }
                                    }
                                    else {
                                        Log.d("STATUS", "No existe el estado Estado EXIST");
                                    }
                                }

                                if (!isDriverNotification) {
                                    mDriverFound = true;
                                    mIdDriverFound = key;

                                    mTimeLimit = 0;
                                    mHandler.postDelayed(mRunnable, 1000);

                                    mDriverFoundLatLng = new LatLng(location.latitude, location.longitude);
                                    mTextViewLookingFor.setText("CONDUCTOR ENCONTRADO\nESPERANDO RESPUESTA");
                                    createClientBooking();

                                    Log.d("DRIVER", "ID: " + mIdDriverFound);
                                }
                                else {
                                    mIsLookingFor = false;
                                    getClosestDriver();
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // INGRESA CUANDO TERMINA LA BUSQUEDA DEL CONDUCTOR EN UN RADIO DE 0.1 KM
                if (!mDriverFound && !mIsLookingFor) {
                    mRadius = mRadius + 0.1f;

                    // NO ENCONTRO NINGUN CONDUCTOR
                    if (mRadius > 5) {

                        // TERMINAR TOTALMENTE LA BUSQUEDA YA QUE NO SE ENCONTRO NINGUN CONDCUTOR

                        if (mListener != null) {
                            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListener);
                        }
                        mTextViewLookingFor.setText("NO SE ENCONTRO UN CONDUCTOR");
                        Toast.makeText(RequestDriverActivity.this, "NO SE ENCONTRO UN CONDUCTOR", Toast.LENGTH_SHORT).show();
                        mIsFinishSearch = true;
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    else {
                        Log.d("REQUEST", "ENTRO GET CLOSETS");
                        getClosestDriver();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void createClientBooking() {

        mGoogleApiProvider.getDirections(mOriginLatLng, mDriverFoundLatLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {

                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    JSONArray legs =  route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");
                    sendNotification(durationText, distanceText);

                } catch(Exception e) {
                    Log.d("Error", "Error encontrado " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });


    }

    private void sendNotificationCancel() {

        if (mIdDriverFound != null) {
            mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                        if (dataSnapshot.hasChild("token")) {
                            String token = dataSnapshot.child("token").getValue().toString();
                            Map<String, String> map = new HashMap<>();
                            map.put("title", "VIAJE CANCELADO");
                            map.put("body",
                                    "El cliente cancelo la solicitud"
                            );
                            FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                            mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                                @Override
                                public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                    if (response.body() != null) {
                                        if (response.body().getSuccess() == 1) {
                                            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                                            startActivity(intent);
                                            finish();
                                            //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else {
                                        Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<FCMResponse> call, Throwable t) {
                                    Log.d("Error", "Error " + t.getMessage());
                                }
                            });
                        }
                        else {
                            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else {
                        Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else {
            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
            startActivity(intent);
            finish();
        }

    }

    private void sendNotification(final String time, final String km) {
        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String token = dataSnapshot.child("token").getValue().toString();
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "SOLICITUD DE SERVICIO A " + time + " DE TU POSICION");
                    map.put("body",
                            "Un cliente esta solicitando un servicio a una distancia de " + km + "\n" +
                                    "Recoger en: " + mExtraOrigin + "\n" +
                                    "Destino: " + mExtraDestination
                    );
                    map.put("idClient", mAuthProvider.getId());
                    map.put("origin", mExtraOrigin);
                    map.put("destination", mExtraDestination);
                    map.put("min", time);
                    map.put("distance", km);
                    map.put("searchById", "false"); //PROBANDO...
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null) {
                                if (response.body().getSuccess() == 1) {
                                    ClientBooking clientBooking = new ClientBooking(
                                            mAuthProvider.getId(),
                                            mIdDriverFound,
                                            mExtraDestination,
                                            mExtraOrigin,
                                            time,
                                            km,
                                            "create",
                                            mExtraOriginLat,
                                            mExtraOriginLng,
                                            mExtraDestinationLat,
                                            mExtraDestinationLng
                                    );

                                    mClientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            checkStatusClientBooking();
                                        }
                                    });
                                    //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("Error", "Error " + t.getMessage());
                        }
                    });
                }
                else {
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null) {
            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListener);
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        mIsFinishSearch = true;
    }

}
