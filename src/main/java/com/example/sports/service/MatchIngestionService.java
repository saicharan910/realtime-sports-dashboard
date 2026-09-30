package com.example.sports.service;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
import com.example.sports.entity.MatchStatus;
import com.example.sports.provider.CricApiClient;
import com.example.sports.repository.MatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class MatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MatchIngestionService.class);

    private final CricApiClient cricApiClient;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchIngestionService(
            CricApiClient cricApiClient,
            MatchRepository matchRepository,
            MatchService matchService,
            SimpMessagingTemplate messagingTemplate) {
        this.cricApiClient = cricApiClient;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.messagingTemplate = messagingTemplate;
    }

    public void ingestCurrentMatches() {
        JsonNode matches = cricApiClient.fetchCurrentMatches();

        for (JsonNode match : matches) {
            try {
                processMatch(match);
            } catch (Exception ex) {
                log.error(
                        "Failed to process one provider match record",
                        ex);
            }
        }
    }

    @Transactional
    protected void processMatch(JsonNode match) {
        String matchId = match.path("id").asText(null);

        if (matchId == null || matchId.isBlank()) {
            log.warn("Skipping provider record without match ID");
            return;
        }

        MatchEntity entity = matchRepository
                .findById(matchId)
                .orElseGet(MatchEntity::new);

        boolean changed = entity.getId() == null;

        entity.setId(matchId);

        String series = match.path("series")
                .asText("International Cricket Series");

        changed |= updateIfChanged(
                entity.getSeries(),
                series,
                entity::setSeries);

        LocalDate matchDate = parseDate(
                match.path("date").asText(null));

        if (!Objects.equals(entity.getMatchDate(), matchDate)) {
            entity.setMatchDate(matchDate);
            changed = true;
        }

        String teamA = extractTeam(match, 0, "Team A");
        String teamB = extractTeam(match, 1, "Team B");

        changed |= updateIfChanged(
                entity.getTeamA(),
                teamA,
                entity::setTeamA);

        changed |= updateIfChanged(
                entity.getTeamB(),
                teamB,
                entity::setTeamB);

        MatchStatus status = determineStatus(match);

        if (!Objects.equals(entity.getStatus(), status)) {
            entity.setStatus(status);
            changed = true;
        }

        String score = extractScore(match);

        changed |= updateIfChanged(
                entity.getScore(),
                score,
                entity::setScore);

        if (!changed) {
            return;
        }

        MatchEntity saved = matchRepository.save(entity);

        MatchResponseDTO dto = matchService.toDto(saved);

        messagingTemplate.convertAndSend(
                "/topic/scores",
                dto);

        log.info(
                "Updated match {}: {} vs {} ({})",
                saved.getId(),
                saved.getTeamA(),
                saved.getTeamB(),
                saved.getStatus());
    }

    private String extractTeam(
            JsonNode match,
            int index,
            String fallback) {
        JsonNode teams = match.path("teams");

        if (teams.isArray() && teams.size() > index) {
            String value = teams.get(index).asText(null);

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return fallback;
    }

    private MatchStatus determineStatus(JsonNode match) {
        if (match.path("matchEnded").asBoolean(false)) {
            return MatchStatus.COMPLETED;
        }

        if (match.path("matchStarted").asBoolean(false)) {
            return MatchStatus.LIVE;
        }

        return MatchStatus.UPCOMING;
    }

    private String extractScore(JsonNode match) {
        JsonNode scores = match.path("score");

        if (scores.isArray() && !scores.isEmpty()) {
            StringBuilder score = new StringBuilder();

            for (JsonNode inning : scores) {
                if (!score.isEmpty()) {
                    score.append(" | ");
                }

                score.append(
                        inning.path("inning")
                                .asText("Innings"))
                        .append(": ")
                        .append(inning.path("r").asText("-"))
                        .append("/")
                        .append(inning.path("w").asText("-"))
                        .append(" (")
                        .append(inning.path("o").asText("-"))
                        .append(" ov)");
            }

            return score.toString();
        }

        String providerStatus = match
                .path("status")
                .asText(null);

        if (providerStatus != null
                && !providerStatus.isBlank()) {
            return providerStatus;
        }

        return "Scheduled";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            log.warn("Invalid match date from provider: {}", value);
            return null;
        }
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