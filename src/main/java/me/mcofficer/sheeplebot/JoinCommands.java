package me.mcofficer.sheeplebot;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

public class JoinCommands implements CommandExecutor {

    private String redirectUri;
    private final String clientId;
    private Properties properties;

    public JoinCommands (Bot bot) {
        this.properties = bot.getProperties();
        try {
           this.redirectUri = java.net.URLEncoder.encode(properties.getProperty("redirectUri"), "ISO-8859-1");
        }
        catch (UnsupportedEncodingException e) {
            System.out.println("Failed to encode redirect URL. Exiting.");
            System.exit(3);
        }
        this.clientId = properties.getProperty("clientId");
    }

    @Command(aliases = {"^join"}, description = "test", usage = "", privateMessages = false)
    public void onJoinCommand(Guild guild, MessageChannel channel, User author, Message msg) {
        if (channel.getIdLong() != (Long.parseLong(properties.getProperty("channelId"))))
            return;
        String args = msg.getContentStripped().replace("^join", "").trim();
        if (args.length() < 1 && properties.getProperty("enableOAuthJoin").equals("1")) {
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
        else if (args.length() > 0) {
            Member member = guild.getMember(author);
            Role role = guild.getRolesByName(args, true).get(0);
            String[] freeRoles = properties.getProperty("freeRoles").split(",");
            for (String freeRole : freeRoles)
                if (Long.parseLong(freeRole) == role.getIdLong())
                    guild.getController().addSingleRoleToMember(member, role).queue(success -> msg.addReaction("👌").queue());
        }
    }


    @Command(aliases = {"^leave"}, description = "leave", privateMessages = false)
    public void onLeaveCommand(Guild guild, MessageChannel channel, User author, Message msg) {
        if (channel.getIdLong() != (Long.parseLong(properties.getProperty("channelId"))))
            return;
        String args = msg.getContentStripped().replace("^leave", "").trim();
        Role role = guild.getRolesByName(args, true).get(0);
        Member member = guild.getMember(author);
        guild.getController().removeSingleRoleFromMember(member, role).queue(success -> msg.addReaction("👌").queue());
    }
}
