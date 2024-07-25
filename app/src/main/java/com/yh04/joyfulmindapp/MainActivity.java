package com.yh04.joyfulmindapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        DatePicker datePicker = findViewById(R.id.datePicker);
        ImageView imgDetail = findViewById(R.id.imgDetail);

        // 현재 날짜로 설정
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // 날짜가 변경될 때 호출됩니다.
                String selectedDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;

                imgDetail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fetchChatData(selectedDate, "대석규"); // 닉네임을 여기에 추가
                    }
                });
            }
        });
    }

    private void fetchChatData(String selectedDate, String nickname) {
        // 선택된 날짜의 시작과 끝을 설정
        Calendar selectedCalendar = Calendar.getInstance();
        String[] dateParts = selectedDate.split("-");
        selectedCalendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        selectedCalendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
        selectedCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));

        Calendar startOfDay = (Calendar) selectedCalendar.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);

        Calendar endOfDay = (Calendar) selectedCalendar.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        db.collection("UserChatting")
                .whereEqualTo("nickname", nickname)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTime())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTime())
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        StringBuilder chatData = new StringBuilder();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String message = document.getString("message");
                            chatData.append(message).append("\n");
                        }

                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra("chatData", chatData.toString());
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // 실패 시 처리
                    }
                });
    }
}