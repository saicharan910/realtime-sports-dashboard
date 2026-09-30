package com.example.sports.service;

import com.example.sports.entity.MatchEntity;
import com.example.sports.repository.MatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Service
public class SportsDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(SportsDataSimulator.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchRepository matchRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String baseUrl;

    public SportsDataSimulator(
            SimpMessagingTemplate messagingTemplate,
            MatchRepository matchRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${cric.api.key}") String apiKey,
            @Value("${cric.api.base-url}") String baseUrl) {

        this.messagingTemplate = messagingTemplate;
        this.matchRepository = matchRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * Poll the external cricket provider.
     *
     * fixedDelay means the next poll starts only after the previous
     * poll has completed. This avoids overlapping API requests.
     */
    @Scheduled(fixedDelayString = "${cric.api.poll-interval-ms:60000}", initialDelayString = "${cric.api.initial-delay-ms:10000}")
    public void fetchAndSaveMatches() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("CRIC_API_KEY is not configured. Skipping cricket data ingestion.");
            return;
        }

        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path("/currentMatches")
                    .queryParam("apikey", apiKey)
                    .queryParam("offset", 0)
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isBlank()) {
                log.warn("CricAPI returned an empty response.");
                return;
            }

            JsonNode root = objectMapper.readTree(response);

            if (!isSuccessfulProviderResponse(root)) {
                log.warn(
                        "CricAPI rejected the current matches request: {}",
                        root.path("reason").asText("unknown provider error"));
                return;
            }

            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray()) {
                log.warn("CricAPI response did not contain a valid data array.");
                return;
            }

            for (JsonNode match : dataArray) {
                processMatch(match);
            }

        } catch (RestClientException e) {
            log.warn("Unable to reach CricAPI: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while ingesting cricket data.", e);
        }
    }

    private void processMatch(JsonNode match) {
        String matchId = match.path("id").asText(null);

        if (matchId == null || matchId.isBlank()) {
            log.warn("Skipping provider record without a match ID.");
            return;
        }

        MatchEntity existing = matchRepository
                .findById(matchId)
                .orElse(null);

        MatchEntity entity = existing != null
                ? existing
                : new MatchEntity();

        boolean changed = existing == null;

        entity.setId(matchId);

        changed |= updateIfChanged(
                entity.getSeries(),
                match.path("series").asText("International Cricket Series"),
                entity::setSeries);

        changed |= updateIfChanged(
                entity.getMatchDate(),
                match.path("date").asText(null),
                entity::setMatchDate);

        String teamA = "Team A";
        String teamB = "Team B";

        JsonNode teams = match.path("teams");

        if (teams.isArray()) {
            if (teams.size() > 0) {
                teamA = teams.get(0).asText("Team A");
            }

            if (teams.size() > 1) {
                teamB = teams.get(1).asText("Team B");
            }
        }

        changed |= updateIfChanged(
                entity.getTeamA(),
                teamA,
                entity::setTeamA);

        changed |= updateIfChanged(
                entity.getTeamB(),
                teamB,
                entity::setTeamB);

        String status = determineStatus(match);

        changed |= updateIfChanged(
                entity.getStatus(),
                status,
                entity::setStatus);

        String score = extractScore(match);

        changed |= updateIfChanged(
                entity.getScore(),
                score,
                entity::setScore);

        /*
         * Do not manufacture win-probability numbers.
         *
         * The provider response currently does not give us a trustworthy
         * probability value, so the field remains null until a real
         * probability source is integrated.
         */
        if (entity.getWinProbability() != null) {
            entity.setWinProbability(null);
            changed = true;
        }

        /*
         * if (changed) {
         * matchRepository.save(entity);
         * 
         * messagingTemplate.convertAndSend(
         * "/topic/scores",
         * entity);
         * 
         * log.info(
         * "Updated match {}: {} vs {} ({})",
         * entity.getId(),
         * entity.getTeamA(),
         * entity.getTeamB(),
         * entity.getStatus());
         * }
         */
        if (changed) {
            matchRepository.save(entity);

            messagingTemplate.convertAndSend(
                    "/topic/scores",
                    entity);

            log.info(
                    "Updated match {}: {} vs {} ({})",
                    entity.getId(),
                    entity.getTeamA(),
                    entity.getTeamB(),
                    entity.getStatus());
        }
    }

    private String determineStatus(JsonNode match) {
        boolean matchStarted = match.path("matchStarted").asBoolean(false);

        boolean matchEnded = match.path("matchEnded").asBoolean(false);

        if (matchEnded) {
            return "COMPLETED";
        }

        if (matchStarted) {
            return "LIVE";
        }

        return "UPCOMING";
    }

    private String extractScore(JsonNode match) {
        JsonNode scores = match.path("score");

        if (scores.isArray() && !scores.isEmpty()) {
            StringBuilder score = new StringBuilder();

            for (JsonNode inning : scores) {
                if (!score.isEmpty()) {
                    score.append(" | ");
                }

                String inningName = inning.path("inning").asText("Innings");

                String runs = inning.path("r").asText("-");

                String wickets = inning.path("w").asText("-");

                String overs = inning.path("o").asText("-");

                score.append(inningName)
                        .append(": ")
                        .append(runs)
                        .append("/")
                        .append(wickets)
                        .append(" (")
                        .append(overs)
                        .append(" ov)");
            }

            return score.toString();
        }

        String providerStatus = match.path("status").asText(null);

        if (providerStatus != null && !providerStatus.isBlank()) {
            return providerStatus;
        }

        return "Scheduled";
    }

    private boolean isSuccessfulProviderResponse(JsonNode root) {
        String status = root.path("status").asText("");

        /*
         * CricAPI normally reports "success".
         * Treat an explicitly reported non-success response as failure.
         */
        return status.isBlank()
                || "success".equalsIgnoreCase(status);
    }

    private boolean updateIfChanged(
            String oldValue,
            String newValue,
            java.util.function.Consumer<String> setter) {
        if (!Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            return true;
        }

        return false;
    }
}