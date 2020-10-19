package ntc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class db {
    protected static String DB_URL="jdbc:oracle:thin:@localhost:1521:XE";
    protected static String DRIVER_URL="oracle.jdbc.OracleDriver";
    protected static String USER="User2";
    protected static String PASSWORD="User2";

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Connection con;
        Class.forName(login.config.getProperty("db.driver"));
        con = DriverManager.getConnection(login.config.getProperty("db.url"), login.config.getProperty("db.user"), login.config.getProperty("db.password"));
        return con;
    }
}
