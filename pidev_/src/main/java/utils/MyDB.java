package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static final String URL = "jdbc:mysql://localhost:3306/mindnest"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true"
            + "&autoReconnect=true"
            + "&failOverReadOnly=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDB instance;

    private MyDB() {
        connect();
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
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

    /**
     * Returns a live connection.
     * If the connection was closed or timed out by MySQL, it reconnects automatically.
     * This fixes the "No operations allowed after connection closed" error.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                System.out.println("[DB] Connection was closed — reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Could not validate connection, trying to reconnect: " + e.getMessage());
            connect();
        }
        return connection;
    }
}