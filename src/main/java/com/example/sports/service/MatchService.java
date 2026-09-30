package com.example.sports.service;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
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

    public List<MatchResponseDTO> getMatchesByStatus(String status) {
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

    private MatchResponseDTO toDto(MatchEntity entity) {
        MatchResponseDTO dto = new MatchResponseDTO();

        dto.setId(entity.getId());
        dto.setTeamA(entity.getTeamA());
        dto.setTeamB(entity.getTeamB());
        dto.setStatus(entity.getStatus());
        dto.setScore(entity.getScore());
        dto.setSeries(entity.getSeries());
        dto.setMatchDate(entity.getMatchDate());
        dto.setCommentary(entity.getCommentary());
        dto.setWinProbability(entity.getWinProbability());
        dto.setLastUpdated(entity.getLastUpdated());

        return dto;
    }
}