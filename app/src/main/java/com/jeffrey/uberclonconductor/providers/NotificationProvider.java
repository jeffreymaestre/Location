package com.jeffrey.uberclonconductor.providers;

import com.jeffrey.uberclonconductor.models.FCMBody;
import com.jeffrey.uberclonconductor.models.FCMResponse;
import com.jeffrey.uberclonconductor.retrofit.IFCMApi;
import com.jeffrey.uberclonconductor.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {
    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {
    }

    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }

}
