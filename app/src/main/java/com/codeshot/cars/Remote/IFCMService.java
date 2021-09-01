package com.codeshot.cars.Remote;

import com.codeshot.cars.Models.DataMessage;
import com.codeshot.cars.Models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAeX-DMRo:APA91bFCAAkt5FZi8gab7QzC52Aq5MIoxbNL3DjVMxNYI8fSH2HpNGx0C52p0RCmpm5oZSaigPwu6sDdhATsCPo8Kbdk2CXw60R8MoORT5--zTCP5niEi4IXzcwVedMCdA43C0VW6vJV"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body DataMessage body);
}
