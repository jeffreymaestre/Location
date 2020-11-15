package com.jeffrey.uberclon.providers;

import com.jeffrey.uberclon.models.FCMBody;
import com.jeffrey.uberclon.models.FCMResponse;
import com.jeffrey.uberclon.retrofit.IFCMApi;
import com.jeffrey.uberclon.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {
    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {
    }

    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }

}
