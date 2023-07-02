package com.googleupdaterunner;

import com.googleupdaterunner.Base64Model;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitAPI {

    @POST("Common")

    //on below line we are creating a method to post our data.
    Call<Base64Model> createPost(@Body Base64Model base64Setting);
}