package com.example.sports.service;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
import com.example.sports.entity.MatchStatus;
import com.example.sports.repository.MatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public List<MatchResponseDTO> getAllMatches() {
        return matchRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<MatchResponseDTO> getMatchesByStatus(MatchStatus status) {
        return matchRepository.findByStatus(status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public MatchResponseDTO getMatchById(String id) {
        return matchRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public MatchResponseDTO toDto(MatchEntity entity) {
        return new MatchResponseDTO(
                entity.getId(),
                entity.getTeamA(),
                entity.getTeamB(),
                entity.getStatus(),
                entity.getScore(),
                entity.getSeries(),
                entity.getMatchDate(),
                entity.getLastUpdated());
    }
}