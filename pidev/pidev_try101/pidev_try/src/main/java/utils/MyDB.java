package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private final String URL = "jdbc:mysql://localhost:3306/mindnest"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";
    private final String USERNAME = "root";
    private final String PASSWORD = "";

    private Connection connection;
    private static MyDB instance;

    private MyDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // optional but safe
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to database");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            e.printStackTrace();
        }
    }

    public static MyDB getInstance() {
        if (instance == null)
            instance = new MyDB();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}