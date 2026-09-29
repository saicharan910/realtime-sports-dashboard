package com.example.sports.service;

import com.example.sports.entity.MatchEntity;
import com.example.sports.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class SportsDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(SportsDataSimulator.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchRepository matchRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${cric.api.key}")
    private String apiKey;

    @Value("${cric.api.base-url}")
    private String baseUrl;

    public SportsDataSimulator(SimpMessagingTemplate messagingTemplate, MatchRepository matchRepository) {
        this.messagingTemplate = messagingTemplate;
        this.matchRepository = matchRepository;
        seedFallbackIfEmpty();
        fetchAndSaveMatches();
    }

    private void seedFallbackIfEmpty() {
        if (matchRepository.count() == 0) {
            MatchEntity m1 = new MatchEntity();
            m1.setId("live-prod-01");
            m1.setSeries("ICC Champions Trophy");
            m1.setMatchDate("2026-09-29");
            m1.setTeamA("India");
            m1.setTeamB("Australia");
            m1.setStatus("LIVE");
            m1.setScore("India: 245/3 (38.2 ov) | Australia: 310/10 (49.1 ov)");
            m1.setWinProbability("India Win Prob: 68% | Australia Win Prob: 32%");
            m1.setCommentary("14:22:10 - Shreyas Iyer hit the ball high going out of stadium and its a SIXXXXXX!\n14:21:45 - Single taken smoothly down to long-on.");
            matchRepository.save(m1);
        }
    }

    @Scheduled(fixedRate = 5000) 
    public void fetchAndSaveMatches() {
        try {
            String url = baseUrl + "/currentMatches?apikey=" + apiKey + "&offset=0";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("data");
            
            if (dataArray.isArray() && dataArray.size() > 0) {
                for (JsonNode match : dataArray) {
                    String matchId = match.path("id").asText();
                    MatchEntity entity = matchRepository.findById(matchId).orElse(new MatchEntity());
                    
                    entity.setId(matchId);
                    entity.setSeries(match.path("series").asText("International Cricket Series"));
                    entity.setMatchDate(match.path("date").asText("2026-09-29"));
                    
                    JsonNode teams = match.path("teams");
                    if (teams.isArray() && teams.size() >= 2) {
                        entity.setTeamA(teams.get(0).asText());
                        entity.setTeamB(teams.get(1).asText());
                    } else {
                        entity.setTeamA("Team A");
                        entity.setTeamB("Team B");
                    }
                    
                    boolean matchStarted = match.path("matchStarted").asBoolean(false);
                    boolean matchEnded = match.path("matchEnded").asBoolean(false);
                    
                    if (matchEnded) {
                        entity.setStatus("COMPLETED");
                    } else if (matchStarted) {
                        entity.setStatus("LIVE");
                    } else {
                        entity.setStatus("UPCOMING");
                    }

                    if (match.path("score").isArray() && match.path("score").size() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode inning : match.path("score")) {
                            sb.append(inning.path("inning").asText()).append(": ")
                              .append(inning.path("r").asText()).append("/")
                              .append(inning.path("w").asText())
                              .append(" (").append(inning.path("o").asText()).append(" ov) | ");
                        }
                        entity.setScore(sb.toString());
                        entity.setWinProbability(entity.getTeamA() + " Win Prob: 55% | " + entity.getTeamB() + " Win Prob: 45% (Live Model)");
                    } else if (match.has("status")) {
                        entity.setScore(match.path("status").asText());
                        entity.setWinProbability("Pre-match / Standby");
                    } else {
                        entity.setScore("Scheduled / Yet to bat");
                        entity.setWinProbability("Awaiting toss");
                    }

                    fetchMatchDetails(matchId, entity);
                    matchRepository.save(entity);
                    messagingTemplate.convertAndSend("/topic/scores", entity);
                }
            }
        } catch (Exception e) {
            log.error("CricAPI Ingestion Exception (Using fallback persistence): {}", e.getMessage());
        }
    }

    private void fetchMatchDetails(String matchId, MatchEntity entity) {
        try {
            String detailUrl = baseUrl + "/match_info?apikey=" + apiKey + "&id=" + matchId;
            String detailResponse = restTemplate.getForObject(detailUrl, String.class);
            JsonNode matchData = objectMapper.readTree(detailResponse).path("data");

            if (!matchData.isMissingNode() && matchData.has("note")) {
                String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String note = matchData.path("note").asText();
                String existing = entity.getCommentary() != null ? entity.getCommentary() : "";
                if (!existing.contains(note)) {
                    entity.setCommentary(timestamp + " - " + note + "\n" + existing);
                }
            }
        } catch (Exception e) {
            log.warn("Detail fetch failed for match ID {}: {}", matchId, e.getMessage());
        }
    }
}