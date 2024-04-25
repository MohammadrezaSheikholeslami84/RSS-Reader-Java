import org.jsoup.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Rss_Reader {
    static ArrayList<String> Urls = new ArrayList<>();
    static File file = new File("data.txt");

    public static void main(String[] args) throws Exception {
        System.out.println("Welcome to Rss Reader!");
        try {
            List_Initialization();
            User_Input();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    //Initialize URLs List By Reading the "data.txt" File
    public static void List_Initialization() throws FileNotFoundException {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("File "+file.getName()+" not created successfully");
            }
        }

        File myObj = new File("data.txt");
        Scanner myReader = new Scanner(myObj);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            String [] Split = data.split(";");
            String Website_Address = Split[1].replace("index.html","");
            Urls.add(Website_Address);
        }
        myReader.close();
    }

    //Input From User Commands
    public static void User_Input() throws Exception {

        Scanner scanner = new Scanner(System.in);
        int choice;

        while (true) {
            System.out.println("Type a Valid Number For Your Desired Action :");
            System.out.println("[1] Show Updates");
            System.out.println("[2] Add URL");
            System.out.println("[3] Remove URL");
            System.out.println("[4] Exit");

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    Show_Updates();
                    break;
                case 2:
                    Add_Url();
                    break;
                case 3:
                    Remove_URL();
                    break;
            }
            if (choice == 4) {
                Update_File("data.txt");
                break;
            }

        }
    }

    //Show Updates Function
    private static void Show_Updates() throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Show Updates For : ");
        System.out.println("[0] All Websites");

        for (int i = 0; i < Urls.size(); i++) {
            System.out.println("["+(i+1)+"]"+ " " + extractPageTitle(fetchPageSource(Urls.get(i))));
        }

        System.out.println("Enter -1 to return");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == -1)
            return;
        else if (choice == 0) {
            for (String url : Urls) {
                System.out.println(extractPageTitle(fetchPageSource(url)) + " : ");
                retrieveRssContent(extractRssUrl(url));
            }
        }
        else
            retrieveRssContent(extractRssUrl(Urls.get(choice-1)));
    }

    // Write All Changes to "data.txt"
    public static void Update_File(String filePath)
    {
        File tempFile = new File(file.getAbsolutePath() + ".tmp");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("File "+file.getName()+" not created successfully");
            }
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {

            for (int i = 0; i < Urls.size(); i++) {
                String Website_Title = extractPageTitle(fetchPageSource(Urls.get(i)));
                String Website_HTML_URL = Urls.get(i) + "index.html";
                String Website_RSS_URL = extractRssUrl(Urls.get(i));
                String text = Website_Title + ";" +Website_HTML_URL + ";" + Website_RSS_URL + ";";
                pw.println(text);
                pw.flush();

            }

            pw.close();

            if (!file.delete()) {
                System.out.println("Could not delete file");
                return;
            }

            if (!tempFile.renameTo(file))
                System.out.println("Could not rename file");

        }
        catch (IOException e) {
            System.out.println("IO Exception Occurred");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Adding Urls to  Urls List
    public static void Add_Url() throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Please Enter Website URL to add : ");

        String add_Url = scanner.nextLine();

        if (Urls.contains(add_Url))
            System.out.println(add_Url + " already exists");

        else {
            Urls.add(add_Url);
            System.out.println("Added "+add_Url+" Successfully");
        }
    }

    // Removing Urls From Urls List
    public static void Remove_URL() throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Please Enter Website URL to remove : ");
        String remove_URL = scanner.nextLine();

        if (Urls.contains(remove_URL)) {
            Urls.remove(remove_URL);
            System.out.println("Removed "+remove_URL+" Successfully");
        }
        else
            System.out.println("Could not find "+remove_URL);
    }


    // Extract Page Title with Html Source Code
    public static String extractPageTitle(String html)
    {
        try
        {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            return  doc.select("title").first().text();
        }
        catch (Exception e)
        {
            return "Error: no title tag found in page source!";
        }
    }

    //Extract Rss Content by Rss URL
    public static void retrieveRssContent(String rssUrl)
    {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(rssXml);
            ByteArrayInputStream input = new ByteArrayInputStream(
                    xmlStringBuilder.toString().getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < 5; ++i) {
                Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent());
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).getTextContent());
                    System.out.println("--------------------");
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }

    //Extract Rss Urls From Website URL
    public static String extractRssUrl(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    //Extract Html Source Code with Website Url
    public static String fetchPageSource(String urlString) throws Exception {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML ,like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }

    private static String toString(InputStream inputStream) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream , "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null)
            stringBuilder.append(inputLine);
        return stringBuilder.toString();
    }

}
