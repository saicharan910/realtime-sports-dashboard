package com.example.sports.service;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
import com.example.sports.event.MatchUpdatedEvent;
import com.example.sports.repository.MatchRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class MatchPersistenceService {

    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchPersistenceService(
            MatchRepository matchRepository,
            ApplicationEventPublisher eventPublisher) {

        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void saveIfChanged(MatchResponseDTO dto) {

        MatchEntity entity = matchRepository.findById(dto.id())
                .orElseGet(MatchEntity::new);

        boolean isNew = entity.getId() == null;

        String effectiveScore = resolveEffectiveScore(
                entity.getScore(),
                dto.score()
        );

        boolean changed = isNew || hasChanged(
                entity,
                dto,
                effectiveScore
        );

        if (!changed) {
            return;
        }

        entity.setId(dto.id());
        entity.setTeamA(dto.teamA());
        entity.setTeamB(dto.teamB());
        entity.setStatus(dto.status());
        entity.setScore(effectiveScore);
        entity.setSeries(dto.series());
        entity.setMatchDate(dto.matchDate());

        MatchEntity saved = matchRepository.save(entity);

        MatchResponseDTO persisted = new MatchResponseDTO(
                saved.getId(),
                saved.getTeamA(),
                saved.getTeamB(),
                saved.getStatus(),
                saved.getScore(),
                saved.getSeries(),
                saved.getMatchDate(),
                saved.getLastUpdated()
        );

        eventPublisher.publishEvent(
                new MatchUpdatedEvent(persisted)
        );
    }

    private boolean hasChanged(
            MatchEntity entity,
            MatchResponseDTO dto,
            String effectiveScore) {

        return !Objects.equals(
                    entity.getTeamA(),
                    dto.teamA()
                )
                || !Objects.equals(
                    entity.getTeamB(),
                    dto.teamB()
                )
                || entity.getStatus() != dto.status()
                || !Objects.equals(
                    entity.getScore(),
                    effectiveScore
                )
                || !Objects.equals(
                    entity.getSeries(),
                    dto.series()
                )
                || !Objects.equals(
                    entity.getMatchDate(),
                    dto.matchDate()
                );
    }

    private String resolveEffectiveScore(
            String existingScore,
            String incomingScore) {

        if (isMeaningfulScore(incomingScore)) {
            return incomingScore.trim();
        }

        return existingScore;
    }

    private boolean isMeaningfulScore(String score) {

        if (score == null) {
            return false;
        }

        return !score.trim().isEmpty()
                && !score.trim().equals("-");
    }
}