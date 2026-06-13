package com.fitplanner.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GroqApiService {
    @POST("v1/chat/completions")
    Call<GroqResponse> getChatCompletion(
        @Header("Authorization") String apiKey,
        @Body GroqRequest request
    );
}
