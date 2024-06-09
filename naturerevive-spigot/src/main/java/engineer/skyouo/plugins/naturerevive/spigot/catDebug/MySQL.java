package engineer.skyouo.plugins.naturerevive.spigot.catDebug;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MySQL {
    private static Connection connection = null;
    public static boolean isConnect(){
        return connection != null;
    }
    public static void connect() {
        String host = "10.10.10.44";
        String port = "3306";
        String DataBase = "test2";
        String UserName = "cat";
        String Passworld = "Joshua1222";

        if(!isConnect()) {
            try {
                Bukkit.getLogger().info("[NatureRevive] 正在連接至MySQL");
                connection = DriverManager.getConnection("jdbc:mysql://" + host +
                        ":" + port + "/" + DataBase + "?useSSL=false&autoReconnect=true&maxReconnects=4", UserName, Passworld);
                Bukkit.getLogger().info("[NatureRevive] 連接成功");
            } catch (SQLException e) {
                Bukkit.getLogger().warning("[NatureRevive] 連接失敗!");
                e.printStackTrace();
            }
        }
    }
    public static void disconnect(){
        if(isConnect()){
            try {
                Bukkit.getLogger().info("[NatureRevive] 正在斷開資料庫連接...");
                connection.close(); //卡在這
                Bukkit.getLogger().info("[NatureRevive] 成功斷開!");
            } catch (SQLException e){
                Bukkit.getLogger().info("[NatureRevive] 發生了未預期的錯誤 請將以下訊息傳送給貓貓");
                e.printStackTrace();
            }
        }
    }
    public static void reconect(){
        String host = "10.10.10.44";
        String port = "3306";
        String DataBase = "test2";
        String UserName = "cat";
        String Passworld = "Joshua1222";
        Bukkit.getLogger().info("[NatureRevive] 連線逾時 正在重新連線...");
        try{
            connection.close();
            connection = DriverManager.getConnection("jdbc:mysql://" + host +
                    ":" + port + "/" + DataBase + "?useSSL=false&autoReconnect=true&maxReconnects=4", UserName, Passworld);
            Bukkit.getLogger().info("[NatureRevive] 重連成功!");
        }catch (Exception e){
            e.printStackTrace();
            Bukkit.getLogger().warning("[NatureRevive] 重新連線失敗!");
        }
    }
    public static void CrateTableData(){
        try{
            try (PreparedStatement statement = connection.prepareStatement
                    ("CREATE TABLE IF NOT EXISTS `SHIP_REGEN_LOG` "
                            + "(INFO TEXT,DATE DATETIME)")) {
                statement.executeUpdate();
                Bukkit.getLogger().info("[NatureRevive] 資表LOGIN_DATE連接成功!");
            }
        }catch (SQLException e){
            Bukkit.getLogger().info("[NatureRevive] 發生了未預期的錯誤 請將以下訊息傳送給貓貓");
            e.printStackTrace();
        }
    }
    public static void create_log_data(String info,Date date) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try{
                    if (connection.isClosed() || !connection.isValid(2)) {
                        reconect();
                    }
                    try (PreparedStatement statement =connection.prepareStatement
                            ("INSERT IGNORE INTO `SHIP_REGEN_LOG`" + "(INFO,DATE) VALUES (?,?)")) {
                        statement.setString(1, info);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = sdf.format(date);
                        statement.setString(2, formattedDate);
                        statement.executeUpdate();
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(NatureRevivePlugin.instance);
    }

}
