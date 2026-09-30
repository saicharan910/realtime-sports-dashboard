package com.example.sports.service;

import com.example.sports.provider.CricApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SportsDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(SportsDataScheduler.class);

    private final MatchIngestionService matchIngestionService;

    public SportsDataScheduler(
            MatchIngestionService matchIngestionService) {
        this.matchIngestionService = matchIngestionService;
    }

    @Scheduled(fixedDelayString = "${cric.api.poll-interval-ms:60000}", initialDelayString = "${cric.api.initial-delay-ms:10000}")
    public void pollMatches() {
        try {
            matchIngestionService.ingestCurrentMatches();
        } catch (CricApiException ex) {
            log.warn(
                    "Cricket provider polling failed: {}",
                    ex.getMessage());
        } catch (Exception ex) {
            log.error(
                    "Unexpected error during cricket provider polling",
                    ex);
        }
    }
}