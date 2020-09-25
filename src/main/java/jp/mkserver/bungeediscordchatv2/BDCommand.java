package jp.mkserver.bungeediscordchatv2;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BDCommand extends Command {

    BungeeDiscordChatV2 plugin;
    public BDCommand(BungeeDiscordChatV2 This) {
        super("bd");
        plugin = This;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) sender;
        if(!plugin.power&&!p.hasPermission("bd.op")){
            plugin.sendMessage(p,plugin.prefix+"§4機能停止中");
            return;
        }
        if(args.length == 0) {
            plugin.sendMessage(p,plugin.prefix + "§2======ヘルプメニュー======");
            plugin.sendMessage(p,plugin.prefix + "§6/bd link §f: Discordアカウントとマイクラをリンクします");
            plugin.sendMessage(p,plugin.prefix + "§6/bd info §f: リンク情報をチェックします");
            plugin.sendMessage(p,plugin.prefix + "§6/bd deny §f: リンク申請を拒否します");
            plugin.sendMessage(p,plugin.prefix + "§6/bd unlink §f: Discordアカウントとマイクラのリンクを削除します");
            if(p.hasPermission("bd.op")) {
                plugin.sendMessage(p,plugin.prefix + "§c/bd on : 機能を起動します");
                plugin.sendMessage(p,plugin.prefix + "§c/bd off : 機能を停止します(緊急時のみ使うこと)");
            }
            if(!plugin.power){
                plugin.sendMessage(p,plugin.prefix+"§4§l機能停止中");
            }
            plugin.sendMessage(p,plugin.prefix+"§2========================");
            plugin.sendMessage(p,plugin.prefix + "§c§lCreated by Mr_IK || v1.0.0");
            return;
        }else if(args.length == 1) {
            if (args[0].equalsIgnoreCase("link")) {
                plugin.inviteLink(p.getUniqueId());
                return;
            } else if (args[0].equalsIgnoreCase("unlink")) {
                plugin.unLink(p.getUniqueId());
                return;
            } else if (args[0].equalsIgnoreCase("deny")) {
                plugin.inviteDeny(p.getUniqueId());
                return;
            } else if (args[0].equalsIgnoreCase("info")) {
                plugin.showPlayerInfo(p);
                return;
            }else if(args[0].equalsIgnoreCase("on")){
                if(!p.hasPermission("bd.op")){
                    plugin.sendMessage(p,plugin.prefix+"§4あなたはこのコマンドを実行できません");
                    return;
                }
                plugin.power = true;
                plugin.config.getConfig().set("power",true);
                plugin.config.saveConfig();
                plugin.sendMessage(p,plugin.prefix+"§aBdiscordの機能を開放しました。");
                return;
            }else if(args[0].equalsIgnoreCase("off")){
                if(!p.hasPermission("bd.op")){
                    plugin.sendMessage(p,plugin.prefix+"§4あなたはこのコマンドを実行できません");
                    return;
                }
                plugin.power = false;
                plugin.config.getConfig().set("power",false);
                plugin.config.saveConfig();
                plugin.sendMessage(p,plugin.prefix+"§aBdiscordの機能を停止しました。");
                return;
            }
        }
    }
}
