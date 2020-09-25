package jp.mkserver.bungeediscordchatv2;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscordAPI extends ListenerAdapter {

    private BungeeDiscordChatV2 plugin;
    private Config config;
    private JDA jda;
    private TextChannel channel;
    private List<WebhookClient> hooklist;

    public DiscordAPI(BungeeDiscordChatV2 plugin,Config config){
        this.plugin = plugin;
        this.config = config;
        hooklist = new ArrayList<>();
        enableBOT();
    }

    public void enableBOT(){
        Configuration conf = config.getConfig();
        String token = conf.getString("token");
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.addEventListeners(this);
        try {
            jda = builder.build();
            jda.awaitReady();
            getChannel();
            webHookLoad();
            EmbedBuilder em = new EmbedBuilder();
            em.setDescription(":ballot_box_with_check: **サーバーが起動しました**");
            em.setColor(Color.CYAN);
            sendMessage(em);
        } catch (LoginException | InterruptedException e) {
            jda = null;
            e.printStackTrace();
        }
    }

    public void getChannel(){
        try {
            channel = jda.getTextChannelById(config.getConfig().getLong("channel_id"));
        }catch (NullPointerException|NumberFormatException e){
            e.printStackTrace();
        }
    }

    public void webHookLoad(){
        for(String url : config.getConfig().getStringList("webhooks")){
            WebhookClient client = new WebhookClientBuilder(url).build();
            hooklist.add(client);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!plugin.power) {
            return;
        }
        if (event.getAuthor().getId().equalsIgnoreCase(jda.getSelfUser().getId())||event.isWebhookMessage()) {
            return;
        }
        User user = event.getAuthor();
        long userid = user.getIdLong();
        if(event.isFromType(ChannelType.PRIVATE)){
            PrivateChannel ch = event.getPrivateChannel();
            String allMsg = event.getMessage().getContentRaw();
            if(!allMsg.startsWith("!bd ")){
                sendHelpMessage(ch);
                return;
            }
            String[] args = allMsg.replaceFirst("!bd ","").split(" ");
            if(args.length==0){
                sendHelpMessage(ch);
                return;
            }

            if(args[0].equalsIgnoreCase("link")&&args.length==2){
                ProxyServer.getInstance().getScheduler().runAsync(plugin,()->{
                    if(plugin.containUserLink(userid)){
                        sendMessage(ch,":warning: あなたは既にリンク済みです！");
                        return;
                    }

                    String player_name = args[1];

                    if(ProxyServer.getInstance().getPlayer(player_name)==null){
                        sendMessage(ch,":warning: そのプレイヤーは現在ログインしていません");
                        return;
                    }

                    ProxiedPlayer p = ProxyServer.getInstance().getPlayer(player_name);
                    UUID p_uuid = p.getUniqueId();
                    if(plugin.containPlayerLink(p_uuid)){
                        sendMessage(ch,":warning: そのプレイヤーは既にリンク済みです！");
                        return;
                    }
                    plugin.linkInvited(p_uuid,userid);
                });

            }else if(args[0].equalsIgnoreCase("unlink")&&args.length==1){

                ProxyServer.getInstance().getScheduler().runAsync(plugin,()->{
                    if(!plugin.containUserLink(userid)){
                        sendMessage(ch,":warning: あなたはまだリンクしていません！");
                        return;
                    }
                    //plugin.unPlayerLink(userid);
                });
            }else if(args[0].equalsIgnoreCase("info")&&args.length==1){

                ProxyServer.getInstance().getScheduler().runAsync(plugin,()->{
                    if(!plugin.containUserLink(userid)){
                        sendMessage(ch,":warning: あなたはリンクしていません");
                        return;
                    }
                    BungeeDiscordChatV2.PlayerData data = plugin.getUserData(userid);
                    if(data==null){
                        sendMessage(ch,":warning: あなたはリンクしていません");
                        return;
                    }
                    sendMessage(ch,":information_source: あなたは "+data.name+"("+data.uuid.toString()+") さんとリンクしています");
                });
            }
        }else if(event.isFromType(ChannelType.TEXT)){
            TextChannel ch = event.getTextChannel();
            if(ch.getIdLong()!=channel.getIdLong()) {
                return;
            }

            ProxyServer.getInstance().getScheduler().runAsync(plugin,()->{
                if(!plugin.containUserLink(userid)){
                    event.getAuthor().openPrivateChannel().complete().sendMessage(":warning: そのチャンネルでチャットするにはマイクラとのリンクが必要です" +
                            "\nヘルプに従ってリンクを行ってください。").queue();
                    sendHelpMessage(event.getAuthor().openPrivateChannel().complete());
                    if(channel.getGuild().getMember(jda.getUserById(jda.getSelfUser().getIdLong())).getPermissions(channel).contains(Permission.MESSAGE_MANAGE)) {
                        event.getMessage().delete().queue();
                    }
                    return;
                }
                plugin.sendBroadcast(plugin.prefix+"§f("+colorCodeEscape(getName(event.getAuthor()))+"§f) "+event.getMessage().getContentRaw());
            });
        }
    }

    public String colorCodeEscape(String msg){
        return msg.replaceAll("§0","").replaceAll("§1","")
                .replaceAll("§2","").replaceAll("§3","")
                .replaceAll("§4","").replaceAll("§5","")
                .replaceAll("§6","").replaceAll("§7","")
                .replaceAll("§8","").replaceAll("§9","")
                .replaceAll("§a","").replaceAll("§b","")
                .replaceAll("§c","").replaceAll("§d","")
                .replaceAll("§e","").replaceAll("§f","")
                .replaceAll("§k","").replaceAll("§l","")
                .replaceAll("§m","").replaceAll("§n","")
                .replaceAll("§o","").replaceAll("§r","")
                .replaceAll("§","");
    }

    public void sendHelpMessage(TextChannel ch){
        EmbedBuilder em = embedBuild("BDiscordV2:ヘルプ", Color.CYAN,"コマンド一覧");
        em.addField("!bd link <ユーザー名>","マイクラアカウントとリンクします",false);
        em.addField("!bd unlink","リンクを解除します",false);
        em.addField("!bd info","現在のリンク状況を確認します",false);
        ch.sendMessage(em.build()).queue();
    }

    public void sendHelpMessage(PrivateChannel ch){
        EmbedBuilder em = embedBuild("BDiscordV2:ヘルプ",Color.CYAN,"コマンド一覧");
        em.addField("!bd link <ユーザー名>","マイクラアカウントとリンクします",false);
        em.addField("!bd unlink","リンクを解除します",false);
        em.addField("!bd info","現在のリンク状況を確認します",false);
        ch.sendMessage(em.build()).queue();
    }

    public void sendMessage(TextChannel ch,String msg){
        msg = msg.replaceAll("@everyone","エブリワン").replaceAll("@here","ヒア");
        ch.sendMessage(msg).queue();
    }

    public void sendMessage(PrivateChannel ch,String msg){
        msg = msg.replaceAll("@everyone","エブリワン").replaceAll("@here","ヒア");
        ch.sendMessage(msg).queue();
    }

    public void sendMessage(User user,String msg){
        msg = msg.replaceAll("@everyone","エブリワン").replaceAll("@here","ヒア");
        if(user.hasPrivateChannel()){
            user.openPrivateChannel().complete().sendMessage(msg).queue();
        }
    }

    public void sendMessage(String msg){
        msg = msg.replaceAll("@everyone","エブリワン").replaceAll("@here","ヒア");
        channel.sendMessage(msg).queue();
    }

    public void sendMessage(EmbedBuilder em){
        channel.sendMessage(em.build()).queue();
    }

    public User getUserFromLongId(long id){
        return jda.getUserById(id);
    }

    public String getName(User user){
        if(channel.getGuild().getMember(user)!=null){
            return channel.getGuild().getMember(user).getEffectiveName();
        }
        return user.getName();
    }

    public String getNameFromLong(long id){
        User user = channel.getJDA().getUserById(id);
        return getName(user);
    }

    public EmbedBuilder embedBuild(String title, Color color,String description){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title, null);
        eb.setColor(color);
        if(description!=null){
            eb.setDescription(description);
        }
        return eb;
    }

    private int hookcount = 0;

    public void webhookMessage(UUID uuid,String msg,String servername){
        if(ProxyServer.getInstance().getPlayer(uuid)==null){
            return;
        }
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(p.getName()+"@"+servername+"サーバー"); // use this username
        builder.setAvatarUrl(skinURL(uuid)); // use this avatar
        builder.setContent(colorCodeEscape(msg));
        hooklist.get(hookcount).send(builder.build());
        hookcount++;
        if(hookcount==hooklist.size()){
            hookcount=0;
        }
    }

    public String skinURL(UUID player_id){
        return "http://cravatar.eu/head/"+player_id.toString()+"/128.png";
    }
}
