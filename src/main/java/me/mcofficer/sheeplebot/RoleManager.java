package me.mcofficer.sheeplebot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class RoleManager {

    private final OkHttpClient client = new OkHttpClient();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String youtubeKey;
    private final String API_BASE_URL = "https://discordapp.com/api/v6/";
    private final Guild guild;
    private final TextChannel channel;
    private final Role youtubeRole;
    private final Role twitterRole;
    private final boolean enableYT;
    private final boolean enableTwitter;

    RoleManager(Bot bot) {
        Properties properties = bot.getProperties();
        this.clientId = properties.getProperty("clientId");
        this.clientSecret = properties.getProperty("clientSecret");
        this.redirectUri = properties.getProperty("redirectUri");
        this.youtubeKey = properties.getProperty("youtubeKey");
        this.guild = bot.getJda().getGuildById(properties.getProperty("guildId"));
        this.channel = guild.getTextChannelById(properties.getProperty("channelId"));
        this.enableYT = properties.getProperty("enableYT").equals("1");
        this.enableTwitter = properties.getProperty("enableTwitter").equals("1");
        this.youtubeRole = guild.getRoleById(properties.getProperty("youtubeRole"));
        this.twitterRole = guild.getRoleById(properties.getProperty("twitterRole"));
    }


    public synchronized void assignRoles(String code, String state) {
        String accessToken = exchangeCode(code);
        if (accessToken == null)
            // Silently fail - if the token is null, an error has already been thrown
            return;

        String service;
        if (state.endsWith("+twitter")) {
            state = state.substring(0, state.length() - 8);
            service = "twitter";
        }
        else if (state.endsWith("+yt")) {
            state = state.substring(0, state.length() - 3);
            service = "yt";
        }
        else if (state.endsWith("+all")) {
            state = state.substring(0, state.length() - 4);
            service = "all";
        }
        else {
            System.out.println("No Service was provided with the request");
            return;
        }

        // Check if the User opening the URL is indeed the one that authorized the bot
        String authorizedUserId = getAuthorizedUserId(accessToken);
        if (authorizedUserId == null || !org.apache.commons.codec.digest.DigestUtils.sha1Hex(authorizedUserId).equals(state)) {
            System.out.println("User ID doesn't match state, aborting");
            return;
        }

        HashMap<String, String> connections = getUserConnections(accessToken);
        boolean youtube = connections.containsKey("youtube") && checkYoutube(connections.get("youtube")) && enableYT
                && (service.equals("yt") || service.equals("all"));
        boolean twitter = connections.containsKey("twitter") && checkTwitter(connections.get("twitter")) && enableTwitter
                && (service.equals("twitter") || service.equals("all"));

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("SheepleBot", "https://github.com/MCOfficer/SheepleBot");

        Member member = guild.getMemberById(authorizedUserId);

        if (youtube) {
            guild.getController().addSingleRoleToMember(member, youtubeRole).queue();
            eb.setDescription(String.format("Assigned Role %s to Member `%s#%s`.",
                    youtubeRole.getName(), member.getUser().getName(), member.getUser().getDiscriminator()));
        }
        if (twitter) {
            guild.getController().addSingleRoleToMember(member, twitterRole).queue();
            eb.setDescription(String.format("Assigned Role %s to Member `%s#%s`.",
                    twitterRole.getName(), member.getUser().getName(), member.getUser().getDiscriminator()));
        }
        if (youtube && twitter)
            eb.setDescription(String.format("Assigned Roles %s and %s to Member `%s#%s`.", youtubeRole.getName(),
                    twitterRole.getName(), member.getUser().getName(), member.getUser().getDiscriminator()));

        channel.sendMessage(eb.build()).queue();
    }


    private synchronized String exchangeCode(String code) {
        FormBody body = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build();
        Request request = new Request.Builder()
                .url(API_BASE_URL + "oauth2/token")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        JSONObject json = null;
        try (Response response = client.newCall(request).execute()){
            json = new JSONObject(response.body().string());
            response.close();
            return json.getString("access_token");
        }
        catch(JSONException | IOException e) {
            if (e instanceof JSONException && json != null)
                System.out.println("Error exchanging code, server returned " + json.toString());
            e.printStackTrace();
        }
        return null;
    }


    private synchronized HashMap<String, String> getUserConnections(String token) {
        Request request = new Request.Builder()
                .url(API_BASE_URL + "users/@me/connections")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        JSONArray array = null;
        JSONObject json;
        String type;
        Boolean verified;
        HashMap<String, String> connections = new HashMap<>();

        try (Response response = client.newCall(request).execute()) {
            array = new JSONArray(response.body().string());
            response.close();
            for (Object o : array){
                json = (JSONObject) o;
                type = json.getString("type");
                verified = json.getBoolean("verified");
                if ( ( type.equals("youtube") || type.equals("twitter") ) && verified )
                    connections.put(type, json.getString("id"));
            }
        }
        catch(JSONException | IOException e) {
            if (e instanceof JSONException && array != null)
                System.out.println("Error retrieving user connections, server returned " + array.toString());
            e.printStackTrace();
        }
        return connections;
    }


    private synchronized boolean checkYoutube(String channelId) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")
                .addPathSegments("youtube/v3/subscriptions")
                .addQueryParameter("part", "id")
                .addQueryParameter("channelId", channelId)
                .addQueryParameter("forChannelId", "UCoxJr0iU-UF39vDbHlZCWjw")
                .addQueryParameter("key", youtubeKey)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        JSONObject json = null;
        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful())
                return false;
            json = new JSONObject(response.body().string());
            response.close();
            if (json.getJSONObject("pageInfo").getInt("totalResults") == 1)
                return true;
        }
        catch(JSONException | IOException e) {
            if (e instanceof JSONException && json != null)
                System.out.println("Error getting youtube subscriptions, server returned " + json.toString());
            e.printStackTrace();
        }
        return false;
    }


    private synchronized boolean checkTwitter(String userId) {
       try {
           Twitter t = new TwitterFactory().getInstance();
           long[] ids = t.getFollowersIDs("BlenderDiscord", -1).getIDs();
           for (long id : ids)
               if (id == Long.parseLong(userId))
                   return true;
       }
       catch (TwitterException e) {
           System.out.println("Error getting Twitter Followers:");
           e.printStackTrace();
       }
       return false;
    }


    private synchronized String getAuthorizedUserId(String token) {
        Request request = new Request.Builder()
                .url(API_BASE_URL + "users/@me")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        JSONObject json = null;
        try (Response response = client.newCall(request).execute()){
            json = new JSONObject(response.body().string());
            return json.getString("id");
        }
        catch (IOException e) {
            System.out.println("Error getting User ID:");
            e.printStackTrace();
        }
        return null;
    }
}
