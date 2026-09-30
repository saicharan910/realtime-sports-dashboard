package com.example.sports.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CricApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public CricApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${cric.api.key:}") String apiKey,
            @Value("${cric.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public JsonNode fetchCurrentMatches() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "CRIC_API_KEY is not configured");
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/currentMatches")
                .queryParam("apikey", apiKey)
                .queryParam("offset", 0)
                .toUriString();

        try {
            String response = restTemplate.getForObject(
                    url,
                    String.class);

            if (response == null || response.isBlank()) {
                throw new CricApiException(
                        "CricAPI returned an empty response");
            }

            JsonNode root = objectMapper.readTree(response);

            if (!isSuccessfulResponse(root)) {
                String reason = root.path("reason")
                        .asText("Unknown provider error");

                throw new CricApiException(
                        "CricAPI rejected the request: " + reason);
            }

            JsonNode data = root.path("data");

            if (!data.isArray()) {
                throw new CricApiException(
                        "CricAPI response did not contain a valid data array");
            }

            return data;

        } catch (RestClientException ex) {
            throw new CricApiException(
                    "Unable to reach CricAPI",
                    ex);
        } catch (CricApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CricApiException(
                    "Unable to parse CricAPI response",
                    ex);
        }
    }

    private boolean isSuccessfulResponse(JsonNode root) {
        String status = root.path("status").asText("");

        return status.isBlank()
                || "success".equalsIgnoreCase(status);
    }
}