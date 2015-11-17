package com.tosnos.cosa;

import java.sql.*;

/**
 * Created by kevin on 8/14/15.
 */
public class DB {
    private static String dbName = "cosa.db";
    private static Connection conn = null;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static Statement getStmt() throws SQLException {
        return conn.createStatement();
    }

    public static PreparedStatement getPStmt(String sql) throws SQLException{
        return conn.prepareStatement(sql);
    }
}
