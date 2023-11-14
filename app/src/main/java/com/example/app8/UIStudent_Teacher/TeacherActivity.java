package com.example.app8.UIStudent_Teacher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dvinfosys.model.ChildModel;
import com.dvinfosys.model.HeaderModel;
import com.dvinfosys.ui.NavigationListView;
import com.example.app8.Login.LoginActivity;
import com.example.app8.NavigationDrawer.Common;
import com.example.app8.NavigationDrawer.SettingsActivity;
import com.example.app8.R;
import com.example.app8.SQLServer.SQLConnection;
import com.example.app8.UIClass.ClassAddActivity;
import com.example.app8.UIClass.ClassListActivity;
import com.example.app8.UIClass.CustomClassListAdapter;
import com.example.app8.UIStudent_Admin.StudentInfo;
import com.example.app8.UIStudent_Admin.StudentListActivity;
import com.google.android.material.navigation.NavigationView;
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

public class TeacherActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private ListView listView;
    private NavigationListView expandable_navigation;
    private Context context;
    private ArrayList<String> accountList;
    private ArrayList<Integer> backgroundList;
    private ArrayList<Integer> studentCountList;
    private CustomClassListAdapter adapter;
    private Connection connection;
    private static final int ADD_ACCOUNT_REQUEST = 1;
    private TextView role;
    private TextView teacherName;
    private File fileexcel = new File("/storage/self/primary/Download"+"/Demo.xls");

    private File filejson = new File("/storage/self/primary/Download"+"/Demo.json");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

        Toolbar toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        context = TeacherActivity.this;


        drawer = findViewById(R.id.drawer_layout);
        listView = findViewById(R.id.listView);
        expandable_navigation = findViewById(R.id.expandable_navigation);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View header= navigationView.getHeaderView(0);
        role= header.findViewById(R.id.role);
        role.setText(R.string.role_teacher);
        Intent intent = getIntent();
        if (intent.hasExtra("username")) {
            String username = intent.getStringExtra("username");

            // Now, you can use 'username' to display it in your UI elements
            // For example, if you have a TextView with ID 'textViewUsername':
            teacherName = header.findViewById(R.id.teacher_name);;
            teacherName.setText(username.toUpperCase());
        }

        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int classId = getClassIdForPosition(position);
                if (classId != -1) {
                    Intent intent = new Intent(TeacherActivity.this, StudentListActivityTeacher.class);
                    intent.putExtra("ClassId", classId); // Truyền class ID
                    startActivityForResult(intent, ADD_ACCOUNT_REQUEST);
                }
            }
        });

        expandable_navigation.init(this)
                .addHeaderModel(new HeaderModel("Home"))
                .addHeaderModel(
                        new HeaderModel("Export", -1,true)
                                .addChildModel(new ChildModel("Excel"))
                                .addChildModel(new ChildModel("Json"))
                )
                .addHeaderModel(new HeaderModel("Import"))
                .addHeaderModel(new HeaderModel("Calendar"))
                .addHeaderModel(new HeaderModel("Log out"))
                .build()
                .addOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                    @Override
                    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                        expandable_navigation.setSelected(groupPosition);

                        //drawer.closeDrawer(GravityCompat.START);
                        if (id == 0) {
                            //Home Menu
                            Common.showToast(context, "Home Select");
                            Intent intent = new Intent(v.getContext(), ClassListActivity.class);
                            startActivity(intent);
                            drawer.closeDrawer(GravityCompat.START);
                        } else if (id == 1){
                            //Common.showToast(context, "Export Select");
                        }
                        else if (id == 2) {
                            Common.showToast(context, "Import Select");
                            drawer.closeDrawer(GravityCompat.START);
                        }
                        else if (id == 3) {
                            Common.showToast(context, "Calendar Select");
                            Intent intent = new Intent(v.getContext(), CalenderActivity.class);
                            startActivity(intent);
                            drawer.closeDrawer(GravityCompat.START);
                        }
                        else if (id == 4) {
                            //Wishlist Menu
                            Common.showToast(context, "Log out Select");
                            Intent intent = new Intent(v.getContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        return false;
                    }
                })
                .addOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                        expandable_navigation.setSelected(groupPosition, childPosition);
                        if (id == 0) {
                            if (connection != null) {
                                try {
                                    Statement statement = connection.createStatement();
                                    ResultSet resultSet = statement.executeQuery("SELECT name_student, code_student, date_of_birth, attendance_date, classId FROM ATTENDANCE");

                                    // Tạo workbook và sheet
                                    HSSFWorkbook workbook = new HSSFWorkbook();
                                    HSSFSheet sheet = workbook.createSheet("Data");

                                    // Lấy thông tin metadata của kết quả truy vấn
                                    ResultSetMetaData metaData = resultSet.getMetaData();
                                    int columnCount = metaData.getColumnCount();

                                    // Tạo row cho tiêu đề cột
                                    HSSFRow headerRow = sheet.createRow(0);
                                    for (int i = 1; i <= columnCount; i++) {
                                        String columnName = metaData.getColumnName(i);
                                        headerRow.createCell(i - 1).setCellValue(columnName);
                                    }

                                    // Tạo row cho từng bản ghi trong kết quả truy vấn
                                    int rowNum = 1;
                                    while (resultSet.next()) {
                                        HSSFRow row = sheet.createRow(rowNum++);
                                        for (int i = 1; i <= columnCount; i++) {
                                            Object value = resultSet.getObject(i);
                                            Log.d("imageData", String.valueOf(value));
                                            row.createCell(i - 1).setCellValue(value != null ? value.toString() : "");
                                        }
                                    }


                                    if (fileexcel.exists()) {
                                        // Delete the existing file
                                        if (fileexcel.delete()) {
                                            Log.d("FileDeleted", "Existing file delete successfully");
                                            Toast.makeText(TeacherActivity.this, "File exists, delete old file", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d("FileDeleted", "Failed to delete existing file");
                                        }
                                    }

                                    // Lưu workbook vào tệp tin
                                    try (FileOutputStream fileOut = new FileOutputStream(fileexcel)) {
                                        workbook.write(fileOut);
                                        Toast.makeText(TeacherActivity.this,"Export Excel Success",Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(TeacherActivity.this,"Error Exporting to Excel",Toast.LENGTH_SHORT).show();
                                    }

                                    // Đóng workbook, ResultSet, statement và connection
                                    try {
                                        workbook.close();
                                        resultSet.close();
                                        statement.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (id == 1) {
                            if(connection!=null){
                                try{
                                    Statement statement = connection.createStatement();
                                    ResultSet resultSet = statement.executeQuery("SELECT name_student, code_student, date_of_birth, attendance_date, classId FROM ATTENDANCE");
                                    //create list
                                    List<StudentInfo> st = new ArrayList<>();
                                    while(resultSet.next()){
                                        String name = resultSet.getString("name_student");
                                        String code = resultSet.getString("code_student");
                                        String dateOfBirth = resultSet.getString("date_of_birth");
                                        String attendance_date = resultSet.getString("attendance_date");
                                        int classId = resultSet.getInt("classId");

                                        StudentInfo st1 = new StudentInfo(name,code,dateOfBirth,attendance_date,classId);
                                        st.add(st1);
                                    }
                                    resultSet.close();
                                    // Export the data to JSON
                                    Gson gson = new Gson();
                                    String jsonData = gson.toJson(st);

                                    if (filejson.exists()) {
                                        // Delete the existing file
                                        if (filejson.delete()) {
                                            Log.d("FileDeleted", "Existing file delete successfully");
                                            Toast.makeText(TeacherActivity.this, "File exists, delete old file", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d("FileDeleted", "Failed to delete existing file");
                                        }
                                    }

                                    try {
                                        FileWriter writer = new FileWriter(filejson);
                                        writer.write(jsonData);
                                        writer.close();
                                        statement.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Toast.makeText(TeacherActivity.this,"Export Json Success",Toast.LENGTH_SHORT).show();
                                }catch (Exception e){
                                    e.printStackTrace();
                                    Toast.makeText(TeacherActivity.this,"Error exporting to JSON",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        drawer.closeDrawer(GravityCompat.START);
                        return false;
                    }
                });

        accountList = new ArrayList<>();
        backgroundList = new ArrayList<>();
        studentCountList = new ArrayList<>();

        adapter = new CustomClassListAdapter(this, accountList, backgroundList, studentCountList);
        listView.setAdapter(adapter);

        connection = SQLConnection.getConnection();

        if (connection != null) {
            loadAccountData();
        } else {
            Toast.makeText(this, "Không thể kết nối đến cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
        }



        Button addAccountButton = findViewById(R.id.addAccountButton);
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TeacherActivity.this, ClassAddActivity.class);
                startActivityForResult(intent, ADD_ACCOUNT_REQUEST);
            }
        });


    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(TeacherActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

//            if(id == R.id.Home) {
//                Intent intent = new Intent(this, ClassListActivity.class);
//                startActivity(intent);
//            } else if (id == R.id.Recognize) {
//                Intent intent = new Intent(this, RecognizeActivity.class);
//                startActivity(intent);
//            } else if (id == R.id.Logout) {
//                Intent intent = new Intent(this, LoginActivity.class);
//                startActivity(intent);
//            }
       /* if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void loadAccountData() {
        accountList.clear();
        backgroundList.clear();
        studentCountList.clear();

        String query = "SELECT [id], [name_class], [name_subject], [background] FROM [PROJECT].[dbo].[CLASS]";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String name_class = resultSet.getString("name_class");
                String name_subject = resultSet.getString("name_subject");
                int backgroundValue = resultSet.getInt("background");
                int studentCount = getStudentCountForClass(resultSet.getInt("id"));

                String accountInfo = name_subject + "\n" + name_class;
                accountList.add(accountInfo);
                backgroundList.add(backgroundValue);
                studentCountList.add(studentCount);
            }
            resultSet.close();
            preparedStatement.close();
            adapter.notifyDataSetChanged();
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi lấy dữ liệu từ cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }

    private int getStudentCountForClass(int classId) {
        String query = "SELECT COUNT(*) AS studentCount FROM THAMGIA WHERE classid = ?";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("studentCount");
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    private int getClassIdForPosition(int position) {
        if (connection != null && position >= 0 && position < accountList.size()) {
            String selectedSubject = accountList.get(position).split("\n")[0];
            String query = "SELECT [id] FROM [PROJECT].[dbo].[CLASS] WHERE [name_subject] = ?";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, selectedSubject);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1; // Trả về -1 nếu không tìm thấy class ID
    }
}
