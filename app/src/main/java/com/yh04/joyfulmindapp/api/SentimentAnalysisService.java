package com.yh04.joyfulmindapp.api;



import com.yh04.joyfulmindapp.model.SentimentRequest;
import com.yh04.joyfulmindapp.model.SentimentResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SentimentAnalysisService {
    @Headers("Content-Type: application/json")
    @POST("/predict")
    Call<SentimentResponse> getSentimentAnalysis(@Body SentimentRequest request);
}

