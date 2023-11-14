package com.example.app8.UIStudent_Teacher;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app8.R;
import com.example.app8.SQLServer.SQLConnection;
import com.example.app8.UIStudent_Admin.CustomStudentListAdapter;
import com.example.app8.UIStudent_Admin.StudentInfo;
import com.google.gson.Gson;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AttendListActivity extends AppCompatActivity {

    private Connection connection;
    private String selectedDate;

    private ListView listView;
    private Button exportButton;
    private CustomStudentListAdapter adapter; // Sử dụng CustomStudentListAdapter
    private ArrayList<String> studentList;
    private ArrayList<byte[]> imageDataList; // Danh sách dữ liệu hình ảnh
    private File fileexcel;
    List<StudentInfo> st;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attend_list);
        // Retrieve the selected date from the Intent
        Intent intent = getIntent();
        int year = intent.getIntExtra("year", -1);
        int month = intent.getIntExtra("month", -1);
        int dayOfMonth = intent.getIntExtra("dayOfMonth", -1);

        // Now you have the selected date, you can use it as needed
        // For example, you can display it in a TextView
        TextView textView = findViewById(R.id.textViewDate);
        listView = findViewById(R.id.listView);
        exportButton = findViewById(R.id.exportButton);


        textView.setText("Selected Date: " + year + "-" + (month + 1) + "-" + dayOfMonth);
        selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
        fileexcel = new File("/storage/self/primary/Download"+"/" + selectedDate + ".xls");
        Log.d("selectedDate", selectedDate);
        connection = SQLConnection.getConnection();
        st = new ArrayList<>();

        studentList = new ArrayList<>();
        imageDataList = new ArrayList<>(); // Khởi tạo danh sách dữ liệu hình ảnh
        adapter = new CustomStudentListAdapter(this, studentList, imageDataList); // Sử dụng CustomStudentListAdapter
        listView.setAdapter(adapter);
        studentList.clear();
        imageDataList.clear();

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    try {
                        // Tạo workbook và sheet
                        HSSFWorkbook workbook = new HSSFWorkbook();
                        HSSFSheet sheet = workbook.createSheet("Data");

                        // Tạo row cho tiêu đề cột
                        HSSFRow headerRow = sheet.createRow(0);
                        // Assuming StudentInfo has corresponding getters for each field (getName, getCode, etc.)
                        String[] columnNames = {"name_student", "code_student", "date_of_birth", "attendance_date", "classId"};
                        for (int i = 0; i < columnNames.length; i++) {
                            headerRow.createCell(i).setCellValue(columnNames[i]);
                        }

                        // Tạo row cho từng bản ghi trong kết quả truy vấn
                        int rowNum = 1;
                        for (StudentInfo student : st) {
                            HSSFRow row = sheet.createRow(rowNum++);
                            row.createCell(0).setCellValue(student.getName());
                            row.createCell(1).setCellValue(student.getCode());
                            row.createCell(2).setCellValue(student.getDateOfBirth());
                            row.createCell(3).setCellValue(student.getAttendance_date());
                            row.createCell(4).setCellValue(student.getClassId());
                        }


                        if (fileexcel.exists()) {
                            // Delete the existing file
                            if (fileexcel.delete()) {
                                Log.d("FileDeleted", "Existing file delete successfully");
                                Toast.makeText(AttendListActivity.this, "File exists, delete old file", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("FileDeleted", "Failed to delete existing file");
                            }
                        }

                        // Lưu workbook vào tệp tin
                        try (FileOutputStream fileOut = new FileOutputStream(fileexcel)) {
                            workbook.write(fileOut);
                            Toast.makeText(AttendListActivity.this, "Export Excel Success", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(AttendListActivity.this, "Error Exporting to Excel", Toast.LENGTH_SHORT).show();
                        }

                        // Đóng workbook, ResultSet, statement và connection
                        try {
                            workbook.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        });

        new DatabaseTask().execute();
    }

    private class DatabaseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (connection != null) {
                try {
                    String query = "SELECT name_student, code_student, date_of_birth, attendance_date, classId FROM ATTENDANCE WHERE attendance_date = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, selectedDate);
                    ResultSet resultSet = preparedStatement.executeQuery();


                    while(resultSet.next()) {
                        String name = resultSet.getString("name_student");
                        String code = resultSet.getString("code_student");
                        String dateOfBirth = resultSet.getString("date_of_birth");
                        String attendance_date = resultSet.getString("attendance_date");
                        int classId = resultSet.getInt("classId");

                        StudentInfo st1 = new StudentInfo(name,code,dateOfBirth,attendance_date,classId);
                        st.add(st1);

                        Log.d("codeAttendList", code);
                        Log.d("classIdAttendList", String.valueOf(classId));
                        // Your other database operations...

                        query = "SELECT name_student, ImageData FROM STUDENT_LIST WHERE code_student = ? AND code_student IN (SELECT code_student FROM THAMGIA WHERE classid = ?)";
                        preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, code);
                        preparedStatement.setInt(2, classId);
                        ResultSet resultSet2 = preparedStatement.executeQuery();

                        while (resultSet2.next()) {
                            String studentName = resultSet2.getString("name_student");
                            studentList.add(studentName);
                            byte[] imageData = resultSet2.getBytes("ImageData");
                            imageDataList.add(imageData);
                            Log.d("name_student", studentName);
                            Log.d("imageData", String.valueOf(imageData));
                        }

                        resultSet2.close();
                        // Example of publishing progress if needed
                        publishProgress();
                    }

                    preparedStatement.close();
                    resultSet.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                // Example of publishing progress with an error message
                publishProgress();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // You can do UI operations before the background task starts
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            // You can update UI based on the progress or handle errors here
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // You can update UI after the background task is completed
            adapter.notifyDataSetChanged();
        }
    }
}