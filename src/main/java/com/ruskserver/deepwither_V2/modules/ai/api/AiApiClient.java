package com.ruskserver.deepwither_V2.modules.ai.api;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AiApiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemma-4-31b-it:generateContent";

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public AiApiClient(String apiKey, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public record ApiResponse(String text, boolean success, String error) {}

    public ApiResponse call(String prompt, boolean thinking) {
        try {
            String url = BASE_URL + "?key=" + apiKey;

            ObjectNode root = mapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            content.put("role", "user");
            ArrayNode parts = content.putArray("parts");
            ObjectNode part = parts.addObject();
            part.put("text", prompt);

            if (thinking) {
                ObjectNode genConfig = root.putObject("generationConfig");
                genConfig.putObject("thinkingConfig").put("thinkingLevel", "high");
            }

            String json = mapper.writeValueAsString(root);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder().url(url).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new ApiResponse("", false, "API error " + response.code() + ": " + responseBody);
                }
                return new ApiResponse(responseBody, true, null);
            }
        } catch (IOException e) {
            return new ApiResponse("", false, "Request failed: " + e.getMessage());
        }
    }
}
