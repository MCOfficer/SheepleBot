package me.mcofficer.sheeplebot;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.UnsupportedEncodingException;

public class JoinCommands implements CommandExecutor {

    private Bot bot;
    private String redirectUri;
    private final String clientId;

    public JoinCommands (Bot bot) {
        this.bot = bot;
        try {
           this.redirectUri = java.net.URLEncoder.encode(bot.getProperties().getProperty("redirectUri"), "ISO-8859-1");
        }
        catch (UnsupportedEncodingException e) {
            System.out.println("Failed to encode redirect URL. Exiting.");
            System.exit(3);
        }
        this.clientId = bot.getProperties().getProperty("clientId");
    }

    @Command(aliases = {"^join"}, description = "test", usage = "", privateMessages = false)
        public void onJoinCommand(Guild guild, MessageChannel channel, User author) {
        String hash = org.apache.commons.codec.digest.DigestUtils.shaHex(author.getId());
        String authUrl = "https://discordapp.com/oauth2/authorize?client_id=" + clientId + "&redirect_uri=" +
                redirectUri + "&response_type=code&scope=identify%20connections&state=" + hash;
        channel.sendMessage(new EmbedBuilder()
                .setTitle("SheepleBot", "https://github.com/MCOfficer/SheepleBot")
                .setDescription("Please visit [this link](" + authUrl + ") and grant the Bot access to your connected accounts." +
                                "\nNote that this Link is personalized and therefor not interchangeable." +
                                "\nYour connected Accounts don't need to be publicly visible.")
                .build()).queue();
    }
}
