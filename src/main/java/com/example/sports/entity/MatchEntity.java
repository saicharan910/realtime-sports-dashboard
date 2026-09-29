package com.example.sports.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cricket_matches")
public class MatchEntity {

    @Id
    private String id;
    private String teamA;
    private String teamB;
    private String status;
    private String score;
    private String series;
    private String matchDate;
    
    @Column(columnDefinition = "TEXT")
    private String commentary;

    @Column(columnDefinition = "TEXT")
    private String winProbability;

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeamA() { return teamA; }
    public void setTeamA(String teamA) { this.teamA = teamA; }
    public String getTeamB() { return teamB; }
    public void setTeamB(String teamB) { this.teamB = teamB; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }
    public String getCommentary() { return commentary; }
    public void setCommentary(String commentary) { this.commentary = commentary; }
    public String getWinProbability() { return winProbability; }
    public void setWinProbability(String winProbability) { this.winProbability = winProbability; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}