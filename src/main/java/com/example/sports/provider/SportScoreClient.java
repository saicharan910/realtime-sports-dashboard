package com.example.sports.provider;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.model.MatchStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class SportScoreClient implements CricketDataProvider {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500L;

    private final RestClient restClient;
    private final String apiKey;

    public SportScoreClient(
        RestClient.Builder builder,
        SimpleClientHttpRequestFactory requestFactory,
        @Value("${sportsdata.api.base-url}") String baseUrl,
        @Value("${sportsdata.api.key}") String apiKey) {

    this.apiKey = apiKey == null ? "" : apiKey.trim();

    this.restClient = builder
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader(
                    HttpHeaders.ACCEPT,
                    MediaType.APPLICATION_JSON_VALUE
            )
            .build();
}

    @Override
    public List<MatchResponseDTO> fetchMatches() {

        SportScoreResponse response = executeWithRetry(() ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/fixtures/")
                                .queryParam("sport", "cricket")
                                .queryParam("limit", 100)
                                .build())
                        .headers(this::applyHeaders)
                        .retrieve()
                        .body(SportScoreResponse.class)
        );

        if (response == null || response.matches() == null) {
            return List.of();
        }

        return response.matches()
                .stream()
                .map(this::toMatchResponse)
                .toList();
    }

    private MatchResponseDTO toMatchResponse(SportScoreMatch match) {

        MatchStatus status = switch (safe(match.status())) {
            case "live" -> MatchStatus.LIVE;
            case "finished" -> MatchStatus.COMPLETED;
            case "upcoming" -> MatchStatus.UPCOMING;
            default -> MatchStatus.UPCOMING;
        };

        String score = buildScore(match);

        if (status == MatchStatus.LIVE && score == null) {
            score = fetchLiveScore(match);
        }

        return new MatchResponseDTO(
                stableId(match),
                safe(match.home()),
                safe(match.away()),
                status,
                score,
                safe(match.competition()),
                match.time() == null
                        ? null
                        : match.time().toLocalDate(),
                LocalDateTime.now()
        );
    }

    private String fetchLiveScore(SportScoreMatch match) {

        String slug = safe(match.slug());

        if (slug.isBlank()) {
            return null;
        }

        try {
            JsonNode detail = executeWithRetry(() ->
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/match/")
                                    .queryParam("sport", "cricket")
                                    .queryParam("slug", slug)
                                    .build())
                            .headers(this::applyHeaders)
                            .retrieve()
                            .body(JsonNode.class)
            );

            return extractScore(detail);

        } catch (RestClientException exception) {
            return null;
        }
    }

    private String extractScore(JsonNode node) {

        if (node == null || node.isNull()) {
            return null;
        }

        String home = findText(node, "home_score");
        String away = findText(node, "away_score");

        if (hasScore(home) || hasScore(away)) {
            if (!hasScore(home)) {
                home = "-";
            }

            if (!hasScore(away)) {
                away = "-";
            }

            return home + " - " + away;
        }

        if (node.isObject()) {
            var fields = node.fields();

            while (fields.hasNext()) {
                JsonNode child = fields.next().getValue();
                String score = extractScore(child);

                if (score != null) {
                    return score;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                String score = extractScore(child);

                if (score != null) {
                    return score;
                }
            }
        }

        return null;
    }

    private String findText(JsonNode node, String fieldName) {

        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }

        if (node.isObject()) {
            var fields = node.fields();

            while (fields.hasNext()) {
                JsonNode child = fields.next().getValue();
                String value = findText(child, fieldName);

                if (value != null) {
                    return value;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findText(child, fieldName);

                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    private String buildScore(SportScoreMatch match) {

        String home = match.home_score();
        String away = match.away_score();

        if (!hasScore(home) && !hasScore(away)) {
            return null;
        }

        if (!hasScore(home)) {
            home = "-";
        }

        if (!hasScore(away)) {
            away = "-";
        }

        return home + " - " + away;
    }

    private boolean hasScore(String value) {

        return value != null
                && !value.isBlank()
                && !value.trim().equals("-");
    }

    private String stableId(SportScoreMatch match) {

        String raw =
                safe(match.home())
                        + "|"
                        + safe(match.away())
                        + "|"
                        + safe(
                                match.time() == null
                                        ? ""
                                        : match.time().toString()
                        )
                        + "|"
                        + safe(match.competition());

        return UUID.nameUUIDFromBytes(
                raw.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private <T> T executeWithRetry(Supplier<T> operation) {

    RestClientException lastException = null;

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

        try {
            return operation.get();

        } catch (RestClientException exception) {

            lastException = exception;

            if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                throw exception;
            }

            try {
                Thread.sleep(RETRY_DELAY_MS * attempt);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "SportScore request interrupted",
                        interruptedException
                );
            }
        }
    }

    throw lastException;
}

private boolean isRetryable(RestClientException exception) {

    if (exception instanceof org.springframework.web.client.HttpStatusCodeException httpException) {
        int status = httpException.getStatusCode().value();

        return status == 429 || status >= 500;
    }

    return true;
}
    private void applyHeaders(HttpHeaders headers) {

        if (!apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record SportScoreResponse(
            String sport,
            String date,
            int count,
            List<SportScoreMatch> matches
    ) {
    }

    private record SportScoreMatch(
            String home,
            String away,
            String home_logo,
            String away_logo,
            String home_score,
            String away_score,
            String status,
            String status_text,
            OffsetDateTime time,
            String competition,
            String competition_logo,
            String url,
            String slug
    ) {
    }
}