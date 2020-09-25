package jp.mkserver.bungeediscordchatv2.sql.dbtypes;

import jp.mkserver.bungeediscordchatv2.sql.DBConnect;
import net.md_5.bungee.api.plugin.Plugin;

import java.sql.*;
import java.util.logging.Level;

public class Type_MySQL extends DBConnect {


    String HOST = null;
    String DB = null;
    String USER = null;
    String PASS = null;
    String PORT = null;
    private Connection con = null;
    private Statement st = null;
    private boolean closed = false;

    private Plugin plugin;

    public Type_MySQL(Plugin plugin,String host, String db, String user, String pass, String port) {
        this.plugin = plugin;
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        this.PORT = port;
    }

    public Connection open() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.con = DriverManager.getConnection("jdbc:mysql://" + this.HOST + ":" + this.PORT +"/" + this.DB + "?useSSL=false", this.USER, this.PASS );
            this.st = con.createStatement();
            return this.con;
        } catch (SQLException var2) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL server, error code: " + var2.getErrorCode());
        } catch (ClassNotFoundException var3) {
            plugin.getLogger().log(Level.SEVERE, "JDBC driver was not found in this machine.");
        }
        return this.con;
    }

    public boolean checkConnection() {
        return this.con != null;
    }

    public void close() {
        try {
            this.con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        closed = true;
    }

    public void changePrepareState(String sql) throws SQLException {
        this.st = con.prepareStatement(sql);
    }

    public PreparedStatement getSafeSt() {
        if(st instanceof PreparedStatement){
            return (PreparedStatement)st;
        }
        return null;
    }

    public Statement getSt() {
        return st;
    }

    public boolean isClosed() {
        return closed;
    }

}
