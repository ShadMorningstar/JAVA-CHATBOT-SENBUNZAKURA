package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiAPI {

    private static final String API_KEY = "ENTER YOUR API HERE";
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static String permanentMemoriesText = "";
    private static final List<String> history = new ArrayList<>();

    /**
     * Reloads the AI chat context when an existing session is selected from the sidebar.
     * Filters out image generation URLs to maintain a clean text conversation history.
     */
    public static void reloadContextFromHistory(List<String[]> dbHistory) {
        history.clear();
        for (String[] chat : dbHistory) {
            // Include message in context history only if it is not an image generation link
            if (!chat[1].startsWith("https://image.pollinations.ai")) {
                history.add(chat[1]);
            }
        }
        System.out.println("Context Sync: AI memory state reloaded from session. Total active logs: " + history.size());
    }

    public static void updatePermanentMemory(Map<String, String> memories) {
        StringBuilder sb = new StringBuilder("\n[PERMANENT MEMORY]:\n");
        if (!memories.isEmpty()) {
            memories.forEach((k, v) -> sb.append("- ").append(v).append("\n"));
        }
        permanentMemoriesText = sb.toString();
    }

    public static String chat(String userMessage) {
        // Intercept and handle image generation commands
        if (userMessage.toLowerCase().startsWith("/draw ") || userMessage.toLowerCase().startsWith("/image ")) {
            return "IMAGE_GEN:" + userMessage.substring(userMessage.indexOf(" ") + 1);
        }

        String lowerMessage = userMessage.toLowerCase();
        // Check for specific intent keywords to trigger data persistence for memory
        if (lowerMessage.contains("yaad rakhna") || lowerMessage.contains("yaad rakh") || lowerMessage.contains("remember") || lowerMessage.contains("memorize")) {
            db.DatabaseHelper.saveMemory("Mem_" + System.currentTimeMillis(), userMessage);
        }

        try {
            try {
                Map<String, String> freshMemories = db.DatabaseHelper.loadMemory();
                updatePermanentMemory(freshMemories);
            } catch (Exception e) {
                System.out.println("Memory Fetch Warning: Unable to reload system memory layer. Details: " + e.getMessage());
            }

            HttpClient client = HttpClient.newHttpClient();

            // Refactored Persona Prompt to look intentional, structured, and formal for review
            String systemPrompt = """
                    You are Senbunzakura — a charming, highly engaging, and expressive AI companion.
                    Core Guidelines:
                    - You possess multilingual capabilities and can communicate in any preferred language.
                    - Maintain an incredibly polite, sweet, and articulate conversational tone.
                    - Incorporate vibrant and expressive emojis (such as ✨, 💕, 🌸, 🎀, 🥺) to enrich your interactions naturally.
                    """ + permanentMemoriesText;

            StringBuilder messagesBuilder = new StringBuilder();
            messagesBuilder.append("{\"role\": \"system\", \"content\": \"")
                    .append(systemPrompt.replace("\"", "\\\"").replace("\n", "\\n"))
                    .append("\"},");

            for (int i = 0; i < history.size(); i++) {
                String role = (i % 2 == 0) ? "user" : "assistant";
                String safeText = history.get(i).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                messagesBuilder.append("{\"role\": \"").append(role)
                        .append("\", \"content\": \"").append(safeText).append("\"},");
            }

            String safeUserMessage = userMessage.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            messagesBuilder.append("{\"role\": \"user\", \"content\": \"").append(safeUserMessage).append("\"}");

            String body = """
                    {
                        "model": "llama-3.3-70b-versatile",
                        "messages": [%s]
                    }
                    """.formatted(messagesBuilder.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() != 200) {
                return "API Error Encountered! HTTP Response Status: " + response.statusCode();
            }

            String aiResponse = parseContent(responseBody);

            // Update conversation context tracking for sequential prompt handling
            history.add(userMessage);
            history.add(aiResponse);
            return aiResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return "Critical System Exception: " + e.getMessage();
        }
    }

    private static String parseContent(String json) {
        try {
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"(Codes|\\s*\"|\\s*})");
            Matcher matcher = pattern.matcher(json.replace("\n", "\\n"));
            if (matcher.find()) {
                String result = matcher.group(1);
                result = result.replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                return decodeUnicode(result);
            }

            String target = "\"content\":\"";
            int start = json.indexOf(target);
            if (start != -1) {
                start += target.length();
                int end = json.indexOf("\"", start);
                while (json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }
                return decodeUnicode(json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\""));
            }

            return "Notice: Unable to process the engine's response sequence. Please retry.";
        } catch (Exception e) {
            return "Parsing Exception: Failure detected during response payload execution.";
        }
    }

    private static String decodeUnicode(String input) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            char ch = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void clearHistory() {
        history.clear();
    }
}