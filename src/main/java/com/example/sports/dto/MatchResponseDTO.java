package com.example.sports.dto;

import java.time.LocalDateTime;

public class MatchResponseDTO {

    private String id;
    private String teamA;
    private String teamB;
    private String status;
    private String score;
    private String series;
    private String matchDate;
    private String commentary;
    private String playerDetails;
    private String winProbability;
    private LocalDateTime lastUpdated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamA() {
        return teamA;
    }

    public void setTeamA(String teamA) {
        this.teamA = teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public void setTeamB(String teamB) {
        this.teamB = teamB;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(String matchDate) {
        this.matchDate = matchDate;
    }

    public String getCommentary() {
        return commentary;
    }

    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }

    public String getPlayerDetails() {
        return playerDetails;
    }

    public void setPlayerDetails(String playerDetails) {
        this.playerDetails = playerDetails;
    }

    public String getWinProbability() {
        return winProbability;
    }

    public void setWinProbability(String winProbability) {
        this.winProbability = winProbability;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}