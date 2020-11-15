package com.jeffrey.uberclon.retrofit;


import com.jeffrey.uberclon.models.FCMBody;
import com.jeffrey.uberclon.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA45O5FFM:APA91bGRqw9mUvPlPFotXdxqbQ01bsU3C6A2BR0n0ac9BSXowlGJLK8EZQomiH9aFnC85faXhhZPqTYi4as4lDNAvF9D02L7Q4pQVwYLoVW0bSigXk-TNn8MTFefkks7CNK73LnOTBYa"
    })
    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);

}
