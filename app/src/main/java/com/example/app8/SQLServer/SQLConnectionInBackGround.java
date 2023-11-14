package com.example.app8.SQLServer;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnectionInBackGround {
    private static final String TAG = "SQLConnection";
    private static String ip = "192.168.1.2"; // Địa chỉ IP của máy chủ SQL Server
    private static String port = "1433"; // Cổng mặc định cho SQL Server
    private static String Classes = "net.sourceforge.jtds.jdbc.Driver"; // Lớp điều khiển JDBC cho SQL Server
    private static String database = "PROJECT"; // Tên cơ sở dữ liệu
    private static String username = "Khanh"; // Tên đăng nhập SQL Server
    private static String password = "123"; // Mật khẩu đăng nhập SQL Server
    private static String url = "jdbc:jtds:sqlserver://" + ip + ":" + port + "/" + database;
    private static Connection connection2 = null;

    // Phương thức để thiết lập và trả về kết nối đến cơ sở dữ liệu
    public static Connection getConnection() {
        if (connection2 == null) {
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Class.forName(Classes);
                            connection2 = DriverManager.getConnection(url, username, password);
                            if (connection2 != null) {
                                Log.d(TAG, "Kết nối cơ sở dữ liệu thành công.");
                            } else {
                                Log.e(TAG, "Kết nối cơ sở dữ liệu thất bại.");
                            }
                        } catch (ClassNotFoundException | SQLException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
                        }
                    }
                });
                thread.start();
                thread.join(); // Chờ luồng kết thúc trước khi trả về connection
                return connection2;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return connection2;
        }
    }
}
