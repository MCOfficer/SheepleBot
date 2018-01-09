package me.mcofficer.sheeplebot;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        new Main();
    }

    private Main() {
        System.out.println("Starting...");

        Properties credentials = loadCredentials();
        System.out.println("Credentials read successfully, starting Bot...\n");

        Bot bot = new Bot(credentials);
    }

    private Properties loadCredentials() {
        Properties credentials = new Properties();
        try {
            credentials.load(new FileReader("sheeplebot.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return credentials;
    }

}