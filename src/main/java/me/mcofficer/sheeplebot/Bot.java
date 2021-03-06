package me.mcofficer.sheeplebot;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JDA3Handler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.Properties;

public class Bot {
    private JDA jda;
    private ServerListener serverListener;
    private RoleManager roleManager;
    private Properties properties;
    private StatusClient statusClient;

    Bot(Properties properties) {
        try {
            this.properties = properties;
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(properties.getProperty("token"))
                    .setGame(Game.listening("^join"))
                    .buildBlocking();
            CommandHandler cmdHandler = new JDA3Handler(jda);
            System.out.println("Bot Instantiation successful");
            serverListener = new ServerListener(this);
            roleManager = new RoleManager(this);
            statusClient = new StatusClient(properties);
            new Thread(() -> statusClient.run()).start();
            cmdHandler.registerCommand(new Commands(properties, statusClient));
            serverListener.main();
        }
        catch (LoginException e) {
            e.printStackTrace();
            jda.shutdown();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public ServerListener getServerListener() {
        return serverListener;
    }

    public Properties getProperties() {
        return properties;
    }

    public JDA getJda() {
        return jda;
    }
}
