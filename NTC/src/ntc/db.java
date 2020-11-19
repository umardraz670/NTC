package ntc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class db {
    protected static String DB_URL="jdbc:oracle:thin:@localhost:1521:XE";
    protected static String DRIVER_URL="oracle.jdbc.OracleDriver";
    protected static String USER="NTCFSD";
    protected static String PASSWORD="fiazNTC5078$";
//    private final static String db_url="jdbc:sqlserver://localhost;instance=mssqlserver;Database=NTC";

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Connection con;
        Class.forName(DRIVER_URL);
        con = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        con.setAutoCommit(false);
        return con;
    }
}
