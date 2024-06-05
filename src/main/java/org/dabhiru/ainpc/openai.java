package org.dabhiru.ainpc;

//package org.dabhiru.pvpbotplugin2;
//ckage org.dabhiru.aibot;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class openai {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "sk-proj-SCZ8qegWu2hEGGWlgbyJT3BlbkFJj7Pqi8BYVJUBO4qVX1Aq";

    public static String getChatResponse(String prompt) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + API_KEY);

            JSONObject json = new JSONObject();
            json.put("model", "gpt-3.5-turbo-0125"); // Specify the model here

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);

            json.put("messages", messages);
            json.put("max_tokens", 4000); // Adjusted max_tokens for better performance and cost

            StringEntity entity = new StringEntity(json.toString());
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject responseJson = new JSONObject(responseBody);

                // Log the full response for debugging
                System.out.println("Response JSON: " + responseJson.toString());

                if (responseJson.has("choices")) {
                    return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                } else if (responseJson.has("error")) {
                    throw new RuntimeException("API Error: " + responseJson.getJSONObject("error").getString("message"));
                } else {
                    throw new RuntimeException("Unexpected API response: " + responseJson.toString());
                }
            }
        }
    }
}

