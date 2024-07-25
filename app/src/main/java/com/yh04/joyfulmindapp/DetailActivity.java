package com.yh04.joyfulmindapp;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yh04.joyfulmindapp.api.RetrofitClientInstance;
import com.yh04.joyfulmindapp.api.SentimentAnalysisService;
import com.yh04.joyfulmindapp.model.SentimentRequest;
import com.yh04.joyfulmindapp.model.SentimentResponse;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private PieChart pieChart;
    private HorizontalBarChart barChart;
    private Map<String, Integer> emotionCountMap = new HashMap<>(); // 감정 카운트를 위한 맵

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        db = FirebaseFirestore.getInstance();
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        String chatData = getIntent().getStringExtra("chatData");

        if (chatData != null) {
            analyzeSentiments(chatData.split("\n"));
        }
    }

    private void analyzeSentiments(String[] messages) {
        for (String message : messages) {
            analyzeSentiment(message);
        }
    }

    private void analyzeSentiment(String text) {
        if (text == null || text.isEmpty()) {
            Log.e("DetailActivity", "Text is null or empty");
            return;
        }

        SentimentRequest request = new SentimentRequest(text);
        SentimentAnalysisService service = RetrofitClientInstance.getRetrofitInstance().create(SentimentAnalysisService.class);
        Call<SentimentResponse> call = service.getSentimentAnalysis(request);

        call.enqueue(new Callback<SentimentResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<SentimentResponse> call, Response<SentimentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Float> predictions = response.body().getPrediction();
                    if (predictions != null) {
                        for (Map.Entry<String, Float> entry : predictions.entrySet()) {
                            emotionCountMap.put(entry.getKey(), emotionCountMap.getOrDefault(entry.getKey(), 0) + entry.getValue().intValue());
                        }
                        displayCharts();
                    } else {
                        Log.e("DetailActivity", "Predictions map is null");
                    }
                } else {
                    Log.e("DetailActivity", "Error: " + response.message());
                    Log.e("DetailActivity", "Error code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e("DetailActivity", "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e("DetailActivity", "Error parsing error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<SentimentResponse> call, Throwable t) {
                Log.e("DetailActivity", "API call failed: " + t.getMessage(), t);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void displayCharts() {
        if (emotionCountMap.isEmpty()) {
            Log.e("DetailActivity", "No emotion data available to display in charts.");
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        String[] emotions = {"fear", "disgust", "surprise", "sadness", "angry", "happiness", "neutral"};

        for (int i = 0; i < emotions.length; i++) {
            float value = emotionCountMap.getOrDefault(emotions[i], 0);
            Log.d("DetailActivity", "Emotion: " + emotions[i] + ", Value: " + value);
            if (i < 4) {
                pieEntries.add(new PieEntry(value, emotions[i]));
            } else {
                barEntries.add(new BarEntry(i, value));
            }
        }

        // Pie Chart
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(15f);
        pieChart.setData(new PieData(pieDataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("감정 분석");
        pieChart.animate();

        // Bar Chart
        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(16f);
        BarData barData = new BarData(barDataSet);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(emotions));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(2000);
    }

}