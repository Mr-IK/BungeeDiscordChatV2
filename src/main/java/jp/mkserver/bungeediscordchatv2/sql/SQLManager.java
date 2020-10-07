package jp.mkserver.bungeediscordchatv2.sql;

import jp.mkserver.bungeediscordchatv2.Config;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

/*
　※注意事項
　MySQLとSQLiteでは少し仕様が違うようです。そのあたりを考慮したコード構成にしてください。
　
　発見できた仕様違い例:
  My:WHEREで、TEXTを検索するとき大文字小文字を区別しない (Mr_IKで登録時、mr_ikで検索するとHITする
  Lite:WHEREで、TEXTを検索するとき大文字小文字を区別する (Mr_IKで登録時、mr_ikで検索するとHITしない
 */

/**
 * SQLManager V3.1 Type-Bungee
 *  ご利用の際はDBConnect Type_MySQL,Type_SQLiteを併用してください。
 *
 * Updated by Mr_IK on 2020/09/25.
 *
 * ChangeLog(更新履歴)
 * Type-Bungee: BungeeCord用に調整。
 * Ver 3.1(〃): SQLインジェクション対策用メソッド executeSafeとquerySafeを追加
 * Ver 3.0(SQM): SQLiteの使用に対応+コード整理
 * Ver 2.0(V2): 内部のアクセスシステムを一部再構築+バグ修正
 *
 * ↓ Created by takatronix ↓
 * Ver 1.0(Original): MySQLへのアクセスとコマンド実行、クエリ取得など基礎機能
 */

public class SQLManager {

    //両方で使用
    private Boolean debugMode = false;
    private Plugin plugin;
    private String conName;

    //SQLite用
    private boolean mode_sqlite = false;
    private String FILE_PATH = null;

    //mysql用
    private boolean connected = false;
    private String HOST = null;
    private String DB = null;
    private String USER = null;
    private String PASS = null;
    private String PORT = null;

    private HashMap<Integer, DBConnect> connects;
    
    private Config config;

    ////////////////////////////////
    //      コンストラクタ
    ////////////////////////////////
    public SQLManager(Plugin plugin, Config config, String name) {
        this.config = config;
        this.connects = new HashMap<>();
        this.plugin = plugin;
        this.conName = name;
        this.connected = false;
        init();
    }

    public void init(){
        loadConfig();

        int result;

        if(mode_sqlite){
            result = Connect(FILE_PATH);
        }else{
            result = Connect(HOST,DB,USER,PASS,PORT);
        }

        if(result == -1){
            plugin.getLogger().warning("データベースにアクセスできませんでした。");
        }

        //テーブル作成はここ
        execute("CREATE TABLE if not exists link_data(" +
                "id int auto_increment not null primary key," +
                "time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "uuid text, " +
                "name text, "+
                "dis_id bigint,"+
                "colortype SMALLINT"+
                ");");
    }


    /////////////////////////////////
    //       設定ファイル読み込み
    /////////////////////////////////
    public void loadConfig(){
        plugin.getLogger().info("データベース設定 ロード開始");
        Configuration conf = config.getConfig();
        boolean sqlpartern = conf.getBoolean("db.sqlite");
        if(sqlpartern){
            plugin.getLogger().info("タイプ: SQLite");
            mode_sqlite = true;
            FILE_PATH = plugin.getDataFolder().getAbsolutePath()+ File.separator+"sqlite.db";
        }else{
            plugin.getLogger().info("タイプ: MySQL");
            HOST = conf.getString("db.mysql.host");
            USER = conf.getString("db.mysql.user");
            PASS = conf.getString("db.mysql.pass");
            PORT = conf.getString("db.mysql.port");
            DB = conf.getString("db.mysql.db");
        }
        plugin.getLogger().info("設定データをロードしました");
    }


    ////////////////////////////////
    //  MySQL用接続
    ////////////////////////////////
    public int Connect(String host, String db, String user, String pass,String port) {
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        this.PORT = port;
        int data = connects.size()+1;
        connects.put(connects.size()+1,new Type_MySQL(plugin,host,db,user,pass,port));
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("MySQLのオープンに失敗しました");
            return -1;
        }
        this.plugin.getLogger().info("[" + this.conName + "] データベースへ接続しました。");
        return data;
    }

    ////////////////////////////////
    //  SQLite用接続
    ////////////////////////////////
    public int Connect(String file_path) {
        this.FILE_PATH = file_path;
        int data = connects.size()+1;
        connects.put(connects.size()+1,new Type_SQLite(plugin,FILE_PATH));
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("SQLiteのオープンに失敗しました");
            return -1;
        }
        this.plugin.getLogger().info("[" + this.conName + "] データベースへ接続しました。");
        return data;
    }
    
    ////////////////////////////////
    //  汎用新規コネクション作成
    ////////////////////////////////
    public DBConnect createConnection(){
        if(mode_sqlite){
            return new Type_SQLite(plugin,FILE_PATH);
        }else{
            return new Type_MySQL(plugin,HOST,DB,USER,PASS,PORT);
        }
    }

    ////////////////////////////////
    //     行数を数える
    ////////////////////////////////
    public int countRows(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT * FROM %s", new Object[]{table})).rs;

        try {
            while(set.next()) {
                ++count;
            }
        } catch (SQLException var5) {
            plugin.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
        }

        return count;
    }

    ////////////////////////////////
    //     レコード数
    ////////////////////////////////
    public int count(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT count(*) from %s", table)).getRs();

        try {
            count = set.getInt("count(*)");

        } catch (SQLException var5) {
            plugin.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
            return -1;
        }

        return count;
    }

    ////////////////////////////////
    //      実行
    ////////////////////////////////
    public boolean execute(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("データベースへのアクセスに失敗しました。");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            connects.get(data).getSt().execute(query);
        } catch (SQLException var3) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース 実行エラー: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;
        }

        connects.get(data).close();
        return ret;
    }

    public boolean executeSafe(String query,String[] rep) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("データベースへのアクセスに失敗しました。");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }
        try {
            connects.get(data).changePrepareState(query);
            int i = 1;
            for(String re : rep){
                connects.get(data).getSafeSt().setString(i,re);
                i++;
            }
            connects.get(data).getSafeSt().execute();
        } catch (SQLException var3) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース 実行エラー: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;
        }

        connects.get(data).close();
        return ret;
    }

    ////////////////////////////////
    //      クエリ
    ////////////////////////////////
    public Query query(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("データベースへのアクセスに失敗しました。");
            return null;
        }
        ResultSet rs = null;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            rs = connects.get(data).getSt().executeQuery(query);
        } catch (SQLException var4) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース クエリエラー: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

        //query.close();
        return new Query(rs,connects.get(data));
    }

    public Query querysafe(String query,String[] rep) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            plugin.getLogger().warning("データベースへのアクセスに失敗しました。");
            return null;
        }
        ResultSet rs = null;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            connects.get(data).changePrepareState(query);
            int i = 1;
            for(String re : rep){
                connects.get(data).getSafeSt().setString(i,re);
                i++;
            }
            rs = connects.get(data).getSafeSt().executeQuery();
        } catch (SQLException var4) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース クエリエラー: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

        //query.close();
        return new Query(rs,connects.get(data));
    }

    public int notClosedConnectionCount(){
        int count = 0;
        for(DBConnect connect : connects.values()){
            if(!connect.isClosed()){
                count++;
            }
        }
        return count;
    }

    public void forceCloseAllConnection(){
        for(DBConnect connect : connects.values()){
            if(!connect.isClosed()){
                connect.close();
            }
        }
    }

    public class Query {
        private ResultSet rs = null;
        private DBConnect connect;

        public Query(ResultSet rs, DBConnect connect){
            this.connect = connect;
            this.rs = rs;
        }

        public ResultSet getRs() {
            return rs;
        }

        public void close(){
            try {
                rs.close();
                connect.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
