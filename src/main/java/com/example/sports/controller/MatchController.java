package com.example.sports.controller;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
import com.example.sports.repository.MatchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchRepository matchRepository;

    public MatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        List<MatchResponseDTO> dtos = matchRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private MatchResponseDTO convertToDTO(MatchEntity entity) {
        MatchResponseDTO dto = new MatchResponseDTO();
        dto.setId(entity.getId());
        dto.setTeamA(entity.getTeamA());
        dto.setTeamB(entity.getTeamB());
        dto.setStatus(entity.getStatus());
        dto.setScore(entity.getScore());
        dto.setSeries(entity.getSeries());
        dto.setMatchDate(entity.getMatchDate());
        dto.setCommentary(entity.getCommentary());
        dto.setLastUpdated(entity.getLastUpdated());
        return dto;
    }
}