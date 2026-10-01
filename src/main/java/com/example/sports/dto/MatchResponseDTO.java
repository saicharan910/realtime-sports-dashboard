package com.example.sports.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.sports.model.MatchStatus;

public record MatchResponseDTO(
        String id,
        String teamA,
        String teamB,
        MatchStatus status,
        String score,
        String series,
        LocalDate matchDate,
        LocalDateTime lastUpdated) {
}