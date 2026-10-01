package com.example.sports.provider;

import com.example.sports.dto.MatchResponseDTO;

import java.util.List;

public interface CricketDataProvider {

    List<MatchResponseDTO> fetchMatches();
}