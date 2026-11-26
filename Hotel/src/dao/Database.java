package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String DB_PATH =
            System.getProperty("user.dir") + "/hotel.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
