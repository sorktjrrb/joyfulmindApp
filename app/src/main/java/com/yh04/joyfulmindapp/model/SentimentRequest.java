package com.yh04.joyfulmindapp.model;

import com.google.gson.annotations.SerializedName;

public class SentimentRequest {
    @SerializedName("input")
    private String input;

    public SentimentRequest(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
