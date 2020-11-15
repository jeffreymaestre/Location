package com.jeffrey.uberclon.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffrey.uberclon.activities.driver.MapDriverBookingActivity;
import com.jeffrey.uberclon.providers.AuthProvider;
import com.jeffrey.uberclon.providers.ClientBookingProvider;
import com.jeffrey.uberclon.providers.GeofireProvider;

public class AcceptReceiver extends BroadcastReceiver {
    private ClientBookingProvider mClientBookingProvider;
    private GeofireProvider mGeofireProvider;
    private AuthProvider mAuthProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mGeofireProvider.removeLocation(mAuthProvider.getId());

        String idClient = intent.getExtras().getString("idClient");
        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(idClient, "accept");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        Intent intent1 = new Intent(context, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient", idClient);
        context.startActivity(intent1);

    }

}
