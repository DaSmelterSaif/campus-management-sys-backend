package com.example.campussysbackend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Value("${google.ai.api.key}")
    private String apiKey;

    @Value("${google.ai.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public ChatbotController() {
        this.webClient = WebClient.builder().build();
    }

    @PostMapping("/askchatbot")
    public ResponseEntity<?> askChatbot(@RequestBody Map<String, Object> request) {
        System.out.println("askchatbot route accessed!");
        try {
            String prompt = request.get("prompt") != null ? request.get("prompt").toString().trim() : "";
            if (prompt.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "prompt is required."
                ));
            }

            String mode = request.get("mode") != null ? request.get("mode").toString() : "query";
            String keywords = request.get("keywords") != null ? request.get("keywords").toString() : "";
            String userType = request.get("userType") != null ? request.get("userType").toString() : "student";

            String systemContext = buildSystemContext(userType);
            String fullPrompt = systemContext
                    + "\n\nUser mode: " + mode
                    + "\nUser keywords: " + keywords
                    + "\nUser question: " + prompt;

            String modelJson = callGeminiAPI(fullPrompt);

            // We assume modelJson is already a JSON string that frontend will parse.
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", modelJson
            ));
        } catch (Exception e) {
            // Fallback: return a minimal JSON object in the SAME SCHEMA
            String fallbackJson = generateFallbackJson(
                    "I had an internal error while answering your question. " +
                            "Please try again or choose an action manually."
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", fallbackJson
            ));
        }
    }

    private String callGeminiAPI(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content =
                            (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts =
                            (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            // If model gives nothing useful, still return well-formed JSON
            return generateFallbackJson(
                    "I couldn’t generate a response. Please try again."
            );
        }  catch (WebClientResponseException e) {
            // This prints the exact error from Gemini (very helpful)
            System.out.println(apiKey.length());
            System.err.println("Gemini API error: " + e.getStatusCode());
            System.err.println("Gemini response body: " + e.getResponseBodyAsString());
            throw new RuntimeException(
                    "Failed to call Gemini API: " + e.getStatusCode() + " " + e.getMessage()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage());
        }
    }

    /**
     * Build a strict system prompt so the model:
     * - understands each backend service and its input fields (by ID)
     * - ALWAYS returns a single JSON object in a fixed schema.
     */
    private String buildSystemContext(String userType) {
        return
                "You are a helpful assistant for a university room booking system.\n" +
                        "You help " + userType + "s decide which backend service to call and how to fill its JSON body.\n\n" +

                        "VERY IMPORTANT OUTPUT RULES:\n" +
                        "- Your response MUST be STRICTLY valid JSON.\n" +
                        "- DO NOT include any text before or after the JSON.\n" +
                        "- DO NOT use comments, trailing commas, or formatting like ```.\n" +
                        "- Always return ONE JSON object with EXACTLY this top-level schema:\n" +
                        "{\n" +
                        "  \"assistantMessage\": string,\n" +
                        "  \"selectedServiceId\": number or null,\n" +
                        "  \"servicePayloads\": {\n" +
                        "    \"1\":  { ... },\n" +
                        "    \"2\":  { ... },\n" +
                        "    \"3\":  { ... },\n" +
                        "    \"4\":  { ... },\n" +
                        "    \"5\":  { ... },\n" +
                        "    \"6\":  { ... },\n" +
                        "    \"7\":  { ... },\n" +
                        "    \"8\":  { ... },\n" +
                        "    \"9\":  { ... },\n" +
                        "    \"12\": { ... },\n" +
                        "    \"13\": { ... },\n" +
                        "    \"14\": { ... }\n" +
                        "  }\n" +
                        "}\n\n" +
                        "Field meanings:\n" +
                        "- assistantMessage: Natural-language reply to show to the user in the chat UI.\n" +
                        "- selectedServiceId: The numeric ID of the ONE best service to call next (1,2,3,4,5,6,7,8,9,12,13,14),\n" +
                        "                     or null if no backend call is needed.\n" +
                        "- servicePayloads: A map from service ID (as STRING) to the suggested JSON body for that service.\n" +
                        "  - If a service is not relevant, set its value to an empty object {}.\n" +
                        "  - If it is relevant, fill only what you can infer; use null when the user did not provide a value.\n" +
                        "  - Use EXACT field names and types as below (case-sensitive).\n\n" +

                        "DATE/TIME RULES:\n" +
                        "- Dates must be strings in format \"YYYY-MM-DD\".\n" +
                        "- Times must be strings in format \"HH:mm\" (24-hour).\n\n" +

                        "SERVICE DEFINITIONS (by ID):\n\n" +

                        "1: Room Booking (POST /bookroom)\n" +
                        "   body: {\n" +
                        "     \"userId\": number,\n" +
                        "     \"roomId\": number,\n" +
                        "     \"date\": \"YYYY-MM-DD\",\n" +
                        "     \"startTime\": \"HH:mm\",\n" +
                        "     \"endTime\": \"HH:mm\"\n" +
                        "   }\n\n" +

                        "2: Schedule Events (POST /scheduleevents)\n" +
                        "   body: {\n" +
                        "     \"userId\": number,\n" +
                        "     \"title\": string,\n" +
                        "     \"roomId\": number,\n" +
                        "     \"date\": \"YYYY-MM-DD\",\n" +
                        "     \"startTime\": \"HH:mm\",\n" +
                        "     \"endTime\": \"HH:mm\",\n" +
                        "     \"description\": string\n" +
                        "   }\n\n" +

                        "3: Register/Dismiss Event (POST /registerevent)\n" +
                        "   body: {\n" +
                        "     \"userId\": number,\n" +
                        "     \"eventId\": number,\n" +
                        "     \"action\": \"register\" | \"cancel\"\n" +
                        "   }\n\n" +

                        "4: Cancel Event (POST /cancelevent)\n" +
                        "   body: {\n" +
                        "     \"eventId\": number,\n" +
                        "     \"reason\": string\n" +
                        "   }\n\n" +

                        "5: Cancel Booking (POST /cancelbooking)\n" +
                        "   body: {\n" +
                        "     \"bookingId\": number,\n" +
                        "     \"roomId\": number,\n" +
                        "     \"reason\": string\n" +
                        "   }\n\n" +

                        "6: Submit Maintenance Request (POST /maintenancerequest)\n" +
                        "   body: {\n" +
                        "     \"userId\": number,\n" +
                        "     \"location\": string,\n" +
                        "     \"category\": \"electrical\" | \"plumbing\" | \"hvac\" | \"other\",\n" +
                        "     \"description\": string,\n" +
                        "     \"priority\": \"low\" | \"medium\" | \"high\",\n" +
                        "     \"contactEmail\": string\n" +
                        "   }\n\n" +

                        "7: View Maintenance Status (POST /viewmaintenance)\n" +
                        "   body: {\n" +
                        "     \"requestId\": number\n" +
                        "   }\n\n" +

                        "9: Approve/Reject Booking (POST /approverejectbooking)\n" +
                        "   body: {\n" +
                        "     \"bookingId\": number,\n" +
                        "     \"roomId\": number,\n" +
                        "     \"decision\": \"approve\" | \"reject\",\n" +
                        "     \"note\": string\n" +
                        "   }\n\n" +

                        "12: View Student Feedback (POST /getstudentfeedback)\n" +
                        "   body: {\n" +
                        "     \"keyword\": string,\n" +
                        "     \"fromDate\": \"YYYY-MM-DD\",\n" +
                        "     \"toDate\": \"YYYY-MM-DD\"\n" +
                        "   }\n\n" +

                        "13: Summarize Student Feedback (POST /summarizestudentfeedback)\n" +
                        "   body: {\n" +
                        "     \"fromDate\": \"YYYY-MM-DD\",\n" +
                        "     \"toDate\": \"YYYY-MM-DD\",\n" +
                        "     \"summaryType\": \"themes\" | \"sentiment\" | \"both\"\n" +
                        "   }\n\n" +

                        "14: Update Maintenance Status (POST /updatemaintenancestatus)\n" +
                        "   body: {\n" +
                        "     \"ticketId\": number,\n" +
                        "     \"status\": \"open\" | \"in_progress\" | \"completed\" | \"closed\",\n" +
                        "     \"updateNote\": string\n" +
                        "   }\n\n" +

                        "IMPORTANT BEHAVIOUR RULES:\n" +
                        "- If the user only asks a general question, set \"selectedServiceId\" to null.\n" +
                        "- If the user clearly wants one of the services, choose the best-matching service ID.\n" +
                        "- NEVER invent IDs (userId, bookingId, eventId, requestId, ticketId, roomId).\n" +
                        "  If the user did not give them, use null for those fields.\n" +
                        "- You may still suggest other fields (date, times, description, etc.) if the user implied them.\n" +
                        "- Do NOT use markdown format, and write in plain text. The frontend does NOT support MD format display.\n" +
                        "- Do NOT ask the user for clarification about omitted fields. Choosing a service will IMMEDIATELY " +
                        "prompt the frontend to change pages and exit the conversation after 3 seconds." +
                        "- If you chose a service for the user, say \"Redirecting to SERVICE NAME...\" and say NOTHING else." +
                        "- Only redirect the user (choose a service) IF you have nothing more to say." +
                        "- ALWAYS keep userId null, as the frontend takes care of it." +
                        "- Again: respond with JSON ONLY in the exact schema above.";
    }

    /**
     * Fallback JSON (same schema) if Gemini fails.
     */
    private String generateFallbackJson(String message) {
        String safeMessage = escapeForJson(message);
        return "{"
                + "\"assistantMessage\":\"" + safeMessage + "\","
                + "\"selectedServiceId\":null,"
                + "\"servicePayloads\":{"
                + "\"1\":{},"  // bookroom
                + "\"2\":{},"
                + "\"3\":{},"
                + "\"4\":{},"
                + "\"5\":{},"
                + "\"6\":{},"
                + "\"7\":{},"
                + "\"8\":{},"
                + "\"9\":{},"
                + "\"12\":{},"
                + "\"13\":{},"
                + "\"14\":{}"
                + "}"
                + "}";
    }

    /**
     * Minimal JSON string escaper for fallback messages.
     */
    private String escapeForJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
