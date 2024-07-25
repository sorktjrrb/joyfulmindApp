package com.yh04.joyfulmindapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class SentimentResponse {
    @SerializedName("prediction")
    private Map<String, Float> prediction;

    public Map<String, Float> getPrediction() {
        return prediction;
    }

    public void setPrediction(Map<String, Float> prediction) {
        this.prediction = prediction;
    }
}
