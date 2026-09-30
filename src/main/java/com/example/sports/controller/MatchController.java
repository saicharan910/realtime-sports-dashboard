package com.example.sports.controller;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.model.MatchStatus;
import com.example.sports.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/live")
    public ResponseEntity<List<MatchResponseDTO>> getLiveMatches() {
        return ResponseEntity.ok(
                matchService.getMatchesByStatus(MatchStatus.LIVE));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MatchResponseDTO>> getUpcomingMatches() {
        return ResponseEntity.ok(
                matchService.getMatchesByStatus(MatchStatus.UPCOMING));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<MatchResponseDTO>> getCompletedMatches() {
        return ResponseEntity.ok(
                matchService.getMatchesByStatus(MatchStatus.COMPLETED));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponseDTO> getMatchById(
            @PathVariable String id) {
        MatchResponseDTO match = matchService.getMatchById(id);

        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(match);
    }
}