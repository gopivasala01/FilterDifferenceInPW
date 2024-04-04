package mainPackage;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OpenJira {
    public static void jiraTicketCreation(StringBuilder description) {
        // Jira credentials
        String username = "gopi.v@beetlerim.com";
        String apiToken = "ATATT3xFfGF04x1ZpYRBAW9aAqWewrWQPSrWsYIGYCEnTKdPec0AbZbewgGVTStF6hjkk6xkS5015LZ7ZcGF5ZdeVS6oxWUGTNyQr9raMfTMswTgjurHDHLzb68idXKFwRf-3CesvDt-Ir0Hq2rHMNuy43Eu4mb9kLNaZoiCmj50bFdednURDYQ=CD7BF22C";
        String dynamicDescription = StringEscapeUtils.escapeJson(description.toString());
        
     // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Add 7 days to the current date
        LocalDate futureDate = currentDate.plusDays(7);

        // Print the current date and future date
        System.out.println("Current Date: " + currentDate);
        System.out.println("Future Date (+7 days): " + futureDate);
        // Jira REST API endpoint for creating an issue
        String jiraUrl = "https://hrgitdepartment.atlassian.net/rest/api/2/issue";
        
     // Specify the board ID where you want to create the issue
        String boardId = "10002";
        
    
        // Format dates as strings
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDateString = currentDate.format(formatter);
        String dueDateString = futureDate.format(formatter);

        // Construct the JSON payload with start date, due date, and other fields
        String jsonPayload = "{"
                + "\"fields\": {"
                + "\"project\": {\"id\": \"" + boardId + "\"},"
                + "\"summary\": \"PropertyWare Reports Filter Differences\","
                + "\"description\": \"" + dynamicDescription + "\","
                + "\"issuetype\": {\"name\": \"Task\"},"
                + "\"customfield_10042\": {\"value\": \"2 - High\"}," // Set the priority to High, change as needed
                + "\"assignee\": {\"id\": \"712020:0fbaf53d-16f6-457b-b81d-62313bf580c7\"}," // Set the assignee username
                + "\"customfield_10015\": \"" + startDateString + "\","
                + "\"duedate\": \"" + dueDateString + "\","
                + "\"customfield_10038\": \"" + dueDateString + "\""
                + "}"
                + "}";
 
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        	 //HttpGet httpGet = new HttpGet(jiraUrl);
            HttpPost httpPost = new HttpPost(jiraUrl);
            
            // Set token-based authentication credentials
            httpPost.addHeader("Authorization", "Basic " + encodeCredentials(username, apiToken));
            httpPost.addHeader("Content-Type", "application/json");
            
            // Set JSON payload
          httpPost.setEntity(new StringEntity(jsonPayload));
            
            // Execute the request
           // HttpResponse response = httpClient.execute(httpPost);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            
            // Handle response
            if (entity != null) {
                System.out.println(EntityUtils.toString(entity));
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String encodeCredentials(String username, String apiToken) {
        String credentials = username + ":" + apiToken;
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
