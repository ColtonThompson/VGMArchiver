package org.nightleaf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // Stores the URL for each page of the SNES game list on Zophar.net
    public static String[] gameList = new String[9];
    // Stores the base game page URL where we can parse music downloads later
    public static List<String> gamePageURLs = new ArrayList<>();
    // Stores the game music download URL from the game page
    public static List<String> gameMusicDownloadURL = new ArrayList<>();

    public static int project2612totalCount = 705;

    public static final String zopharBaseURL = "https://www.zophar.net";
    public static int zopharGameCount = 0;

    public static void main(String[] args)
    {
        System.out.println("Building game lists...");
        initGameLists();
        for (int i = 0; i < gameList.length; i++)
        {
            parseGameList(gameList[i]);
        }
        System.out.println("Parsed " + zopharGameCount + " games from Zophar.net");
        if (!gamePageURLs.isEmpty())
        {
            System.out.println("Building music download links...");
            for (String game : gamePageURLs) {
                parseGamePage(game);
            }
        }
        System.out.println("Parsed " + gameMusicDownloadURL.size() + " music archives for download");
        System.out.println("Downloading music...");
        try {
            downloadZophar();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds all the URLs for each page of game lists (1-9)
     */
    private static void initGameLists()
    {
        for (int i = 0; i < gameList.length; i++) {
            gameList[i] = "https://www.zophar.net/music/nintendo-snes-spc?page=" + (i + 1);
        }
    }

    /**
     * Parses each game list and adds each game page URL to our gamePageURLs
     * @param url
     *      The URL for the game page
     */
    private static void parseGameList(String url)
    {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = con.getResponseCode();
            //System.out.println("Response code: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String html = response.toString();
            Document doc = Jsoup.parse(html);

            for (Element member : doc.select(".name")) {
                Element name = member.select(".name a").first();
                if (name == null) continue;
                String gameURL = name.attr("href");
                String fullURL = zopharBaseURL + gameURL;
                addGamePage(fullURL);
                zopharGameCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseGamePage(String url)
    {
        System.out.println("Parsing game at " + url);
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = con.getResponseCode();
            //System.out.println("Response code: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String html = response.toString();
            Document doc = Jsoup.parse(html);

            Element massDownload = doc.select("#mass_download a").first();
            if (massDownload != null) {
                String massDownloadURL = massDownload.attr("href");
                addGameMusicDownload(massDownloadURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadProject2612()
    {
        System.out.println("Downloading from Project 2612!");
        int count = 0;
        for (int i = 0; i < project2612totalCount; i++) {
            String FILE_URL = "https://project2612.org/download.php?id=" + i;
            String FILE_NAME = "src/archives/project2612/" + i + ".zip";
            try {
                System.out.println("Downloading " + i + "/" + project2612totalCount);
                downloadUsingNIO(FILE_URL, FILE_NAME);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void downloadZophar() throws IOException
    {
        // If the list is empty, no need to download
        if (gameMusicDownloadURL.isEmpty())
            return;
        int count = 0;
        for (String url : gameMusicDownloadURL)
        {
            String BASE_FILE_PATH = "src/archives/zophar/";
            String decodedURL = decode(url);
            String FILE_NAME = BASE_FILE_PATH + decodedURL.substring( decodedURL.lastIndexOf('/')+1, decodedURL.length());
            try {
                System.out.println("Downloading " + url + " - (" + count + "/" + gameMusicDownloadURL.size() + ")");
                downloadUsingNIO(url, FILE_NAME);
                count++;
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
        }
    }

    private static void addGamePage(String url)
    {
        gamePageURLs.add(url);
    }

    private static void addGameMusicDownload(String url)
    {
        gameMusicDownloadURL.add(url);
    }

    private static void downloadUsingStream(String urlStr, String file) throws IOException
    {
        URL url = new URL(urlStr);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count=0;
        while((count = bis.read(buffer,0,1024)) != -1)
        {
            fis.write(buffer, 0, count);
        }
        System.out.println("File downloaded " + file);
        fis.close();
        bis.close();
    }

    private static void downloadUsingNIO(String urlStr, String file) throws IOException
    {
        URL url = new URL(urlStr);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        int responseCode = huc.getResponseCode();
        if (responseCode != 200) {
            System.out.println("File didn't exist at " + urlStr + ", skipping!");
            return;
        }
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        System.out.println("File downloaded " + file);
        fos.close();
        rbc.close();
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}