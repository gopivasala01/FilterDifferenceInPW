package mainPackage;

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

public class OpenJira {
    public static void jiraTicketCreation(StringBuilder description) {
        // Jira credentials
        String username = "gopi.v@beetlerim.com";
        String apiToken = "ATATT3xFfGF0gtwtIMi9mWsxcHcIH3M8TzGieQ-gunU91Zmm9fYfJ8ZlGedfBn76w0d-104dziWiTzH-91Gy4ozqbrn9J1QFU7waas0Atc4uFoAqVLP4jSRE_9Dpy3dCuH7JCwBryQ-5RDrq-iMLzYXvqhEiHXBG72Nx_AGreH8ijyqPBIZB_6o=83E77EC0";
        
        // Jira REST API endpoint for creating an issue
        String jiraUrl = "https://hrgitdepartment.atlassian.net/rest/api/2/issue";
        
     // Specify the board ID where you want to create the issue
        String boardId = "10002";
        
        String jsonPayload = "{"
                + "\"fields\": {"
                + "\"project\": {\"id\": \"" + boardId + "\"},"
                + "\"summary\": \"PropertyWare Reports Filter Differences\","
                + "\"description\": \"" + description.toString() + "\","
                + "\"issuetype\": {\"name\": \"Task\"},"
                + "\"customfield_10042\": {\"value\": \"2 - High\"}," // Set the priority to High, change as needed
                + "\"assignee\": {\"name\": \"Ratna\"}" // Set the assignee username
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
