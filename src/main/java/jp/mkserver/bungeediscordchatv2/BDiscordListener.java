package jp.mkserver.bungeediscordchatv2;

import jp.mkserver.bungeediscordchatv2.japanizer.JapanizeType;
import jp.mkserver.bungeediscordchatv2.japanizer.Japanizer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;

public class BDiscordListener implements Listener {

    BungeeDiscordChatV2 plugin;
    public BDiscordListener(BungeeDiscordChatV2 This) {
        plugin = This;
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (e.isCommand()) {
            return;
        }
        String name = player.getName();
        String sname = ((ProxiedPlayer) e.getSender()).getServer().getInfo().getName();
        String msg = plugin.discord.colorCodeEscape(e.getMessage());
        String msgs = "";
        if(plugin.lunachat) {
            msgs = Japanizer.japanize(msg, JapanizeType.GOOGLE_IME);
            if(!msgs.equalsIgnoreCase("")){
                msg = msg +" ("+msgs+")";
            }
        }

        plugin.discord.webhookMessage(player.getUniqueId(),msg,sname);

        msg = ChatColor.translateAlternateColorCodes('&', e.getMessage());
        if(!msgs.equalsIgnoreCase("")){
            msg = msg +" §6("+msgs+")";
        }
        for ( String server : ProxyServer.getInstance().getServers().keySet() ) {
            if ( server.equals(player.getServer().getInfo().getName()) ) {
                continue;
            }
            ServerInfo info = ProxyServer.getInstance().getServerInfo(server);
            for ( ProxiedPlayer players : info.getPlayers() ) {
                players.sendMessage(TextComponent.fromLegacyText("§9[§b"+sname+"§9] §e"+name+"§a: §r"+msg));
            }
        }
        ProxyServer.getInstance().getLogger().info("<"+name+"@"+sname+"> "+msg);
    }

    @EventHandler
    public void onLogout(PlayerDisconnectEvent e){
        ProxiedPlayer player = e.getPlayer();
        String name = player.getName();
        EmbedBuilder em = new EmbedBuilder();
        em.setDescription(":x: **" + name + " さんがログアウトしました**");
        em.setColor(Color.ORANGE);
        plugin.discord.sendMessage(em);
    }
    @EventHandler
    public void onLogin(PostLoginEvent e) {
        String name = e.getPlayer().getName();
        plugin.loadPlayer(e.getPlayer());
        EmbedBuilder em = new EmbedBuilder();
        em.setDescription(":bangbang: **" + name + " さんがログインしました**");
        em.setColor(Color.GREEN);
        plugin.discord.sendMessage(em);
    }
}
