package com.example.sports.scheduler;

import com.example.sports.service.MatchIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SportsDataScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(SportsDataScheduler.class);

    private final MatchIngestionService ingestionService;

    public SportsDataScheduler(MatchIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(
            fixedDelayString = "${sportsdata.poll-interval-ms}",
            initialDelayString = "${sportsdata.initial-delay-ms}"
    )
    public void poll() {

        try {
            ingestionService.ingest();
        } catch (Exception e) {
            log.error("Sports data ingestion failed", e);
        }
    }
}