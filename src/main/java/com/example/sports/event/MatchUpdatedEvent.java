package com.example.sports.event;

import com.example.sports.dto.MatchResponseDTO;

public record MatchUpdatedEvent(MatchResponseDTO match) {
}