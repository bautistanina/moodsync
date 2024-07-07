import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.List;

public class APIClient {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String getChatbotResponse(String conversationContext) {
        try {
            String escapedContext = escapeString(conversationContext);

            String requestBody = "{"
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"" + escapedContext + "\"}],"
                    + "\"model\": \"mixtral-8x7b-32768\","
                    + "\"temperature\": 1,"
                    + "\"max_tokens\": 32768,"
                    + "\"top_p\": 1,"
                    + "\"stream\": false,"
                    + "\"stop\": null"
                    + "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            if (!jsonResponse.has("choices")) {
                throw new JSONException("JSONObject[\"choices\"] not found.");
            }
            JSONArray choicesArray = jsonResponse.getJSONArray("choices");
            JSONObject firstChoice = choicesArray.getJSONObject(0);
            JSONObject messageObject = firstChoice.getJSONObject("message");
            String content = messageObject.getString("content");

            return content;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error: Unable to get response from the chatbot.";
        } catch (JSONException e) {
            e.printStackTrace();
            return "Error: Unexpected response format.";
        }
    }

    public static String generateTitleFromMessages(String conversationContext) {
        try {
            String escapedContext = escapeString(conversationContext);
    
            String requestBody = "{"
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"Generate a relevant title from the user's input with a minimum of 3 words." + escapedContext + "\"}],"
                    + "\"model\": \"mixtral-8x7b-32768\","
                    + "\"temperature\": 1,"
                    + "\"max_tokens\": 13,"
                    + "\"top_p\": 1,"
                    + "\"stream\": false,"
                    + "\"stop\": null"
                    + "}";
    
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            JSONObject jsonResponse = new JSONObject(response.body());
            if (!jsonResponse.has("choices")) {
                throw new JSONException("JSONObject[\"choices\"] not found.");
            }
            JSONArray choicesArray = jsonResponse.getJSONArray("choices");
            JSONObject firstChoice = choicesArray.getJSONObject(0);
            JSONObject messageObject = firstChoice.getJSONObject("message");
            String content = messageObject.getString("content");
    
            return truncateToFiveWords(content);
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
            return "Error: Unable to generate title.";
        }
    }
    

    public static String generateTitleFromMessages(List<String> messages) {
        String combinedMessages = String.join(" ", messages);
        return generateTitleFromMessages(combinedMessages);
    }

    private static String escapeString(String input) {
        return input.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
    }

    private static String truncateToFiveWords(String input) {
        String[] words = input.split("\\s+");
        if (words.length <= 10) {
            return input;
        }
        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            truncated.append(words[i]);
            if (i < 4) {
                truncated.append(" ");
            }
        }
        return truncated.toString();
    }

    public static String getMoodQuote(String conversationContext) {
        try {
            String escapedContext = escapeString(conversationContext);

            String requestBody = "{"
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"After analyzing your current feelings, I'll identify the predominant emotion and offer a supportive quote. Focus on these emotions: Anger, Sadness, Disgust, Joy, Fear, Anxiety, Embarrassment, Ennui, Envy, Guilt, Hope, Confusion, Pride, Longing, Awe." + escapedContext + "\"}],"
                    + "\"model\": \"mixtral-8x7b-32768\","
                    + "\"temperature\": 1,"
                    + "\"max_tokens\": 500,"
                    + "\"top_p\": 1,"
                    + "\"stream\": false,"
                    + "\"stop\": null"
                    + "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            System.out.println("Response JSON: " + jsonResponse.toString(4)); // Print the JSON response for debugging

            if (!jsonResponse.has("choices")) {
                throw new JSONException("JSONObject[\"choices\"] not found.");
            }

            JSONArray choicesArray = jsonResponse.getJSONArray("choices");
            if (choicesArray.length() > 0) {
                JSONObject firstChoice = choicesArray.getJSONObject(0);
                if (firstChoice.has("message")) {
                    JSONObject messageObject = firstChoice.getJSONObject("message");
                    if (messageObject.has("content")) {
                        return messageObject.getString("content");
                    } else {
                        throw new JSONException("JSONObject[\"content\"] not found.");
                    }
                } else {
                    throw new JSONException("JSONObject[\"message\"] not found.");
                }
            } else {
                throw new JSONException("JSONArray[\"choices\"] is empty.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error: Unable to get response from the chatbot.";
        } catch (JSONException e) {
            e.printStackTrace();
            return "Error: Unexpected response format.";
        }
    }

    public static String detectMood(String moodInput) {
        try {
            String escapedContext = escapeString(moodInput.trim());
    
            String requestBody = "{"
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"Detect the mood from the input and provide it as a single word ending with a period. Only use one of the following moods: Anger, Sadness, Disgust, Joy, Fear, Anxiety, Embarrassment, Ennui, Envy, Guilt, Hope, Confusion, Pride, Longing, Awe. If the mood is not an exact match, provide the closest mood from the list. Ensure accurate detection."
 + escapedContext + "\"}],"
                    + "\"model\": \"mixtral-8x7b-32768\","
                    + "\"temperature\": 1,"
                    + "\"max_tokens\": 3,"  // Limit to a small number to ensure single-word response
                    + "\"top_p\": 1,"
                    + "\"stream\": false,"
                    + "\"stop\": null"
                    + "}";
    
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            JSONObject jsonResponse = new JSONObject(response.body());
            System.out.println("Response JSON: " + jsonResponse.toString(4)); // Print the JSON response for debugging
    
            if (!jsonResponse.has("choices")) {
                throw new JSONException("JSONObject[\"choices\"] not found.");
            }
    
            JSONArray choicesArray = jsonResponse.getJSONArray("choices");
            if (choicesArray.length() > 0) {
                JSONObject firstChoice = choicesArray.getJSONObject(0);
                if (firstChoice.has("message")) {
                    JSONObject messageObject = firstChoice.getJSONObject("message");
                    if (messageObject.has("content")) {
                        return messageObject.getString("content").trim().toLowerCase();
                    } else {
                        throw new JSONException("JSONObject[\"content\"] not found.");
                    }
                } else {
                    throw new JSONException("JSONObject[\"message\"] not found.");
                }
            } else {
                throw new JSONException("JSONArray[\"choices\"] is empty.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "error";
        } catch (JSONException e) {
            e.printStackTrace();
            return "error";
        }
    }
}    