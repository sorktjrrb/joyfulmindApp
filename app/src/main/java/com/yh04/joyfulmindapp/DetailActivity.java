package com.yh04.joyfulmindapp;

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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
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

import android.graphics.Color;

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

        // 감정 수치 총합 계산
        int totalCount = emotionCountMap.values().stream().mapToInt(Integer::intValue).sum();

        List<PieEntry> pieEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        String[] emotions = {"fear", "disgust", "surprise", "sadness", "angry", "happiness"};

        // 감정 수치를 퍼센트로 변환
        Map<String, Float> emotionPercentageMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : emotionCountMap.entrySet()) {
            emotionPercentageMap.put(entry.getKey(), (entry.getValue() / (float) totalCount) * 100);
        }

        // 상위 3개의 감정을 파이 차트에 추가 (neutral 제외)
        emotionPercentageMap.entrySet().stream()
                .filter(entry -> !"neutral".equals(entry.getKey())) // neutral 제외
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> pieEntries.add(new PieEntry(entry.getValue(), entry.getKey())));

        // 모든 감정을 바 차트에 추가
        for (int i = 0; i < emotions.length; i++) {
            float value = emotionPercentageMap.getOrDefault(emotions[i], 0f);
            barEntries.add(new BarEntry(i, value));
        }

        // Pie Chart
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(15f); // 글자 크기 키우기
        pieDataSet.setValueFormatter(new PercentFormatter()); // 퍼센트 표시
        pieChart.setData(new PieData(pieDataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("감정 분석");
        pieChart.setCenterTextSize(20f); // 센터 텍스트 크기 키우기
        pieChart.animate();
        pieChart.setEntryLabelTextSize(15f); // 항목 레이블 텍스트 크기 키우기
        pieChart.invalidate(); // 차트 강제 업데이트

        // Bar Chart
        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(16f); // 글자 크기 키우기
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f); // 바 굵기 줄이기
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(emotions));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextSize(15f); // x축 텍스트 크기 키우기
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(2000);
        barChart.getLegend().setTextSize(15f); // 범례 텍스트 크기 키우기
        barChart.invalidate(); // 차트 강제 업데이트
    }

    // 퍼센트 포맷터 클래스
    public static class PercentFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%.1f%%", value);
        }
    }
}