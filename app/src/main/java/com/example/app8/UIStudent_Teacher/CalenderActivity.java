package com.example.app8.UIStudent_Teacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app8.AI_Processor.TestActivity;
import com.example.app8.R;

public class CalenderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calender);
        CalendarView calendarView = findViewById(R.id.calenderView);
        final TextView selectedDay = findViewById(R.id.selectedDay);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int dayOfMonth) {
                // Create an Intent
                Intent intent = new Intent(CalenderActivity.this, AttendListActivity.class);

                // Pass the selected date as extras
                intent.putExtra("year", year);
                intent.putExtra("month", month);
                intent.putExtra("dayOfMonth", dayOfMonth);

                // Start the TestActivity
                startActivity(intent);
            }
        });
    }
}