package me.mcofficer.sheeplebot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerListener {

    private final Bot bot;

    public ServerListener (Bot bot) {
        this.bot = bot;
    }

    public void main() {
        try {
            ServerSocket serverSocket = new ServerSocket(2208);
            while (true) {
                String outputtext = "Thank you for authenticating. Your Role(s) should've been assigned.";
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String s;
                short count = 0;
                while ((s = in.readLine()) != null) {
                    if (s.startsWith("GET /?code=")) {
                        String[] sa = s.split(" ")[1].replace("/?code=", "").split("&state=");
                        bot.getRoleManager().assignRoles(sa[0], sa[1]);
                        count++;
                    }
                    else if (s.startsWith("GET /?state=")) {
                        String[] sa = s.split(" ")[1].replace("/?state=", "").split("&code=");
                        bot.getRoleManager().assignRoles(sa[1], sa[0]);
                        count++;
                    }
                    if (s.isEmpty())
                        break;
                }
                if (count == 0)
                    outputtext = "Looks like something went wrong... Did you access this page without authenticating?";

                out.write("HTTP/1.0 200 OK\r\n");
                out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
                out.write("Server: Apache/0.8.4\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("Content-Length: 59\r\n");
                out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
                out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
                out.write("\r\n");
                out.write("<TITLE>SheepleBot</TITLE>");
                out.write("<P> " + outputtext + "</P>");

                out.close();
                in.close();
                clientSocket.close();
            }
        }
        catch (IOException e) {
            System.out.println("Error while listening to port:");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
