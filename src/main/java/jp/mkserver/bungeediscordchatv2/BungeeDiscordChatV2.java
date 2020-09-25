package jp.mkserver.bungeediscordchatv2;

import jp.mkserver.bungeediscordchatv2.commands.ReplyCommand;
import jp.mkserver.bungeediscordchatv2.commands.TellCommand;
import jp.mkserver.bungeediscordchatv2.sql.SQLManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public final class BungeeDiscordChatV2 extends Plugin {

    public boolean power = true;
    public boolean lunachat = true;
    protected Config config;
    protected SQLManager sql;
    protected DiscordAPI discord;
    public String prefix = "§7§l[§e§lB§b§lDiscordV2§7§l]§r";
    public HashMap<UUID,Long> linkInvite;
    public HashMap<String, String> history;
    public HashMap<UUID,PlayerData> cookie;
    public HashMap<Long,PlayerData> cookie_;


    @Override
    public void onEnable() {
        // Plugin startup logic
        config = new Config(this);
        linkInvite = new HashMap<>();
        history = new HashMap<>();
        cookie = new HashMap<>();
        cookie_ = new HashMap<>();
        lunachat = config.getConfig().getBoolean("lunachat");
        power = config.getConfig().getBoolean("power");
        sql = new SQLManager(this,config,"BDiscordV2");
        discord = new DiscordAPI(this,config);
        getProxy().getPluginManager().registerCommand(this, new BDCommand(this));
        //tell commandを置き換える
        for ( String command : new String[]{"tell", "msg", "message", "m", "w", "t"}) {
            getProxy().getPluginManager().registerCommand(this, new TellCommand(this, command));
        }
        //reply commandを置き換える
        for ( String command : new String[]{"reply", "r"}) {
            getProxy().getPluginManager().registerCommand(this, new ReplyCommand(this, command));
        }
        getProxy().getPluginManager().registerListener(this, new BDiscordListener(this));
        loadPlayerAll();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        EmbedBuilder em = new EmbedBuilder();
        em.setDescription(":no_entry: **サーバーが停止しました**");
        em.setColor(Color.RED);
        discord.sendMessage(em);
    }

    public boolean containPlayerLink(UUID p_uuid){
        String exe = "SELECT * FROM link_data WHERE uuid = '" + p_uuid.toString() + "';";
        SQLManager.Query qu = sql.query(exe);
        ResultSet rs = qu.getRs();
        if (rs != null) {
            try {
                if(rs.next()) {
                    qu.close();
                    return true;
                }
                qu.close();
            } catch (SQLException e) {
                e.printStackTrace();
                qu.close();
                return false;
            }
        }
        return false;
    }

    public void loadPlayer(ProxiedPlayer p){
        ProxyServer.getInstance().getScheduler().runAsync(this,()->{
           if(containPlayerLink(p.getUniqueId())){
               PlayerData data = getUserData(p.getUniqueId());
               if(data!=null){
                   cookie.put(p.getUniqueId(),data);
                   cookie_.put(data.dis_id,data);
               }
           }
        });
    }


    public void loadPlayerAll(){
        ProxyServer.getInstance().getScheduler().runAsync(this,()->{
            for(ProxiedPlayer p : ProxyServer.getInstance().getPlayers()){
                if(containPlayerLink(p.getUniqueId())){
                    PlayerData data = getUserData(p.getUniqueId());
                    if(data!=null){
                        cookie.put(p.getUniqueId(),data);
                        cookie_.put(data.dis_id,data);
                    }
                }
            }
        });
    }

    public boolean containUserLink(long dis_id){
        String exe = "SELECT * FROM link_data WHERE dis_id = " + dis_id + ";";
        SQLManager.Query qu = sql.query(exe);
        ResultSet rs = qu.getRs();
        if (rs != null) {
            try {
                if(rs.next()) {
                    qu.close();
                    return true;
                }
                qu.close();
            } catch (SQLException e) {
                e.printStackTrace();
                qu.close();
                return false;
            }
        }
        return false;
    }

    public PlayerData getUserData(UUID uuid){
        String exe = "SELECT * FROM link_data WHERE uuid = '" + uuid + "';";
        SQLManager.Query qu = sql.query(exe);
        ResultSet rs = qu.getRs();
        if (rs != null) {
            try {
                if(rs.next()) {
                    PlayerData data = new PlayerData();
                    data.name = rs.getString("name");
                    data.uuid = uuid;
                    data.dis_id = rs.getLong("dis_id");
                    qu.close();
                    return data;
                }
                qu.close();
            } catch (SQLException e) {
                e.printStackTrace();
                qu.close();
                return null;
            }
        }
        return null;
    }

    public PlayerData getUserData(long dis_id){
        String exe = "SELECT * FROM link_data WHERE dis_id = " + dis_id + ";";
        SQLManager.Query qu = sql.query(exe);
        ResultSet rs = qu.getRs();
        if (rs != null) {
            try {
                if(rs.next()) {
                    PlayerData data = new PlayerData();
                    data.name = rs.getString("name");
                    data.uuid = UUID.fromString(rs.getString("uuid"));
                    data.dis_id = dis_id;
                    qu.close();
                    return data;
                }
                qu.close();
            } catch (SQLException e) {
                e.printStackTrace();
                qu.close();
                return null;
            }
        }
        return null;
    }

    public void putPlayerLink(String p_name,UUID p_uuid,long dis_id){
        ProxyServer.getInstance().getScheduler().runAsync(this,()->{
            if(containPlayerLink(p_uuid)){
                return;
            }
            PlayerData data = new PlayerData();
            data.uuid = p_uuid;
            data.name = p_name;
            data.dis_id = dis_id;
            cookie.put(p_uuid,data);
            sql.execute("INSERT INTO link_data (name,uuid,dis_id)  VALUES ('"+p_name+"','"+p_uuid.toString()+"',"+dis_id+");");
        });
    }

    public void unPlayerLink(UUID p_uuid){
        ProxyServer.getInstance().getScheduler().runAsync(this,()->{
            if(!containPlayerLink(p_uuid)){
                return;
            }
            long id = cookie.get(p_uuid).dis_id;
            cookie_.remove(id);
            cookie.remove(p_uuid);
            sql.execute("DELETE FROM link_data WHERE uuid = '"+p_uuid+"';");
        });
    }

    public void linkInvited(UUID uuid,long dis_id){
        if(ProxyServer.getInstance().getPlayer(uuid)==null){
            return;
        }
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if(linkInvite.containsKey(uuid)){
            discord.sendMessage(discord.getUserFromLongId(dis_id),":warning: "+getPlayerName(p)+"さんは他プレイヤーからのリンク申請を確認中です。");
            return;
        }
        discord.sendMessage(discord.getUserFromLongId(dis_id), ":checkered_flag: リンク申請を送信しました。ゲーム内で承認してください。");
        sendMessage(p, prefix+" §e"+discord.getNameFromLong(dis_id)+" §aさんからリンク申請が届きました。");
        sendMessage(p, prefix+"§aリンクする場合/bd link しない場合は/bd deny を実行してください。");
        linkInvite.put(uuid,dis_id);
    }

    public void unLink(UUID uuid){
        ProxyServer.getInstance().getScheduler().runAsync(this,()-> {
            if (ProxyServer.getInstance().getPlayer(uuid) == null) {
                return;
            }
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
            if (!containPlayerLink(uuid)) {
                sendMessage(p, prefix+"§cあなたはリンクしていません。");
                return;
            }
            sendMessage(p, prefix+"§aリンクを解除しました。");
            unPlayerLink(p.getUniqueId());
        });
    }

    public void inviteLink(UUID uuid){
        ProxyServer.getInstance().getScheduler().runAsync(this,()-> {
            if (ProxyServer.getInstance().getPlayer(uuid) == null) {
                return;
            }
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
            if (!linkInvite.containsKey(uuid)) {
                sendMessage(p, prefix + "§c現在リンク申請は届いていません。");
                return;
            }
            long dis_id = linkInvite.get(p.getUniqueId());
            putPlayerLink(p.getName(), uuid, dis_id);
            sendMessage(p, prefix + "§e" + discord.getNameFromLong(dis_id) + " §aさんとリンクしました。");
            discord.sendMessage(discord.getUserFromLongId(dis_id), ":link: " + getPlayerName(p) + " さんとリンクが完了しました。");
            linkInvite.remove(uuid);
        });
    }

    public void inviteDeny(UUID uuid){
        if(ProxyServer.getInstance().getPlayer(uuid)==null){
            return;
        }
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if(!linkInvite.containsKey(uuid)){
            sendMessage(p, prefix+"§c現在リンク申請は届いていません。");
            return;
        }
        long dis_id = linkInvite.get(p.getUniqueId());
        sendMessage(p, prefix+"§e"+discord.getNameFromLong(dis_id)+" §cさんからのリンクを拒否しました。");
        discord.sendMessage(discord.getUserFromLongId(dis_id),":warning: "+getPlayerName(p)+" さんはリンクを拒否しました。");
        linkInvite.remove(uuid);
    }

    public void showPlayerInfo(ProxiedPlayer p){
        ProxyServer.getInstance().getScheduler().runAsync(this,()-> {
            if (!containPlayerLink(p.getUniqueId())) {
                sendMessage(p, prefix+"§cあなたはリンクしていません。");
                return;
            }
            PlayerData data = getUserData(p.getUniqueId());
            if (data == null) {
                sendMessage(p, prefix+"§cあなたはリンクしていません。");
                return;
            }
            sendMessage(p, prefix + "§aあなたは §e" + discord.getNameFromLong(data.dis_id) + " §aさんとリンクしています");
        });
    }

    public String getPlayerName(ProxiedPlayer p){
        return p.getName()+"("+p.getUniqueId().toString()+")";
    }

    public void sendMessage(ProxiedPlayer p,String msg){
        p.sendMessage(new TextComponent( msg ));
    }

    public void sendBroadcast(String msg){
        ProxyServer.getInstance().broadcast(new TextComponent(msg));
    }

    public void putHistory(String reciever, String sender) {
        history.put(reciever, sender);
    }

    public String getHistory(String reciever) {
        return history.get(reciever);
    }


    class PlayerData{
        UUID uuid;
        String name;
        long dis_id;
    }
}
