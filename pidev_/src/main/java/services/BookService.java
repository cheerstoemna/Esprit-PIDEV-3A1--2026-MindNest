package services;

import models.Book;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    private static final String API_KEY = "AIzaSyB-yY-7QWJUp0pSxfWwC80LUV1TRIexmNQ"; // replace with your API key

    // Fetch books based on any keyword (quiz category)
    public List<Book> getBooksByCategory(String category) {
        List<Book> books = new ArrayList<>();

        try {
            // Use the category string as the search query
            String query = URLEncoder.encode(category, "UTF-8");
            String urlString = "https://www.googleapis.com/books/v1/volumes?q=" + query
                    + "&maxResults=5&key=" + API_KEY;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray items = json.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject volumeInfo = items.getJSONObject(i).getJSONObject("volumeInfo");

                    String title = volumeInfo.optString("title", "No title");
                    JSONArray authorsArray = volumeInfo.optJSONArray("authors");
                    String authors = "Unknown";
                    if (authorsArray != null) {
                        authors = authorsArray.join(", ").replace("\"", "");
                    }

                    String previewLink = volumeInfo.optString("previewLink", "");

                    books.add(new Book(title, authors, previewLink));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (books.isEmpty()) {
            books.add(new Book("No books found for '" + category + "'", "N/A", ""));
        }

        return books;
    }
}