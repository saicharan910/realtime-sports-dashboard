package com.example.sports.service;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.provider.CricketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(MatchIngestionService.class);

    private final CricketDataProvider cricketDataProvider;
    private final MatchPersistenceService persistenceService;

    public MatchIngestionService(
            CricketDataProvider cricketDataProvider,
            MatchPersistenceService persistenceService) {

        this.cricketDataProvider = cricketDataProvider;
        this.persistenceService = persistenceService;
    }

    public void ingest() {

        List<MatchResponseDTO> matches =
                cricketDataProvider.fetchMatches();

        log.info("Fetched {} cricket matches", matches.size());

        for (MatchResponseDTO match : matches) {
            persistenceService.saveIfChanged(match);
        }
    }
}