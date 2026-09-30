package com.example.sports.dto;

import com.example.sports.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

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