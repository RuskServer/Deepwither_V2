package com.ruskserver.deepwither_V2.modules.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiResponseParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public String parse(String rawResponse) {
        try {
            JsonNode root = mapper.readTree(rawResponse);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                JsonNode error = root.path("error");
                return error.isMissingNode() ? "空のレスポンス" : "APIエラー: " + error.path("message").asText();
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray()) return "空のレスポンス";

            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                if (p.path("thought").asBoolean(false)) continue;
                sb.append(p.path("text").asText());
            }

            String result = sb.toString().trim();
            return result.isEmpty() ? "空のレスポンス" : result;
        } catch (Exception e) {
            return "応答の解析に失敗しました: " + e.getMessage();
        }
    }
}
