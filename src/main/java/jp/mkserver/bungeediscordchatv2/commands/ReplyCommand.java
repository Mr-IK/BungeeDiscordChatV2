package jp.mkserver.bungeediscordchatv2.commands;

import jp.mkserver.bungeediscordchatv2.BungeeDiscordChatV2;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * BungeeJapanizeMessengerのreplyコマンド実装クラス
 * @author ucchy
 */
public class ReplyCommand extends TellCommand {

    private BungeeDiscordChatV2 bdc;

    public ReplyCommand(BungeeDiscordChatV2 bdc, String name) {
        super(bdc, name);
        this.bdc = bdc;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        String recieverName = bdc.getHistory(sender.getName());

        // 引数が無いときは、現在の会話相手を表示して終了する。
        if ( args.length == 0 ) {
            if ( recieverName != null ) {
                sendMessage(sender, ChatColor.LIGHT_PURPLE +
                        "現在の会話相手： " + recieverName);
            } else {
                sendMessage(sender, ChatColor.LIGHT_PURPLE +
                        "現在の会話相手は設定されていません。");
            }
            return;
        }

        // 送信先プレイヤーの取得。取得できないならエラーを表示して終了する。
        if ( recieverName == null ) {
            sendMessage(sender, ChatColor.RED +
                    "メッセージ送信先が見つかりません。");
            return;
        }
        ProxiedPlayer reciever = bdc.getProxy().getPlayer(
                bdc.getHistory(sender.getName()));
        if ( reciever == null ) {
            sendMessage(sender, ChatColor.RED +
                    "メッセージ送信先" + recieverName + "が見つかりません。");
            return;
        }

        // 送信メッセージの作成
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            str.append(args[i] + " ");
        }
        String message = str.toString().trim();

        // 送信
        sendPrivateMessage(sender, reciever, message);
    }
}
