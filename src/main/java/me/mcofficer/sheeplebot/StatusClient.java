package me.mcofficer.sheeplebot;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

class StatusClient {

    private final String statusApi;
    public boolean checked = false;
    public boolean overload;
    public String framesRemaining;
    public String connectedClients;
    public String framesRendering;
    public String activeProjects;
    public Instant lastChecked;
    public int sinceLastSuccess;

    StatusClient(Properties properties) {
        statusApi = properties.getProperty("statusApi");
    }

    void run() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            URL url = new URL(statusApi);
            while(true) {
                try {
                    Document document = documentBuilder.parse(url.openStream());
                    Element stats = (Element) document.getElementsByTagName("stats").item(0);
                    overload = Boolean.valueOf(stats.getAttribute("overload"));
                    framesRemaining = stats.getAttribute("frames_remaining");
                    connectedClients = stats.getAttribute("connected_clients");
                    framesRendering = stats.getAttribute("frames_rendering");
                    activeProjects = stats.getAttribute("active_projects");
                    lastChecked = Instant.now();
                    sinceLastSuccess = 0;
                    checked = true;
                }
                catch (IOException | SAXException | NullPointerException e) {
                    e.printStackTrace();
                    sinceLastSuccess++;
                }
                Thread.sleep(420000); // 7 [m] * 60 [s] * 1000 [milis] = 420 000
            }
        }
        catch (ParserConfigurationException | MalformedURLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
