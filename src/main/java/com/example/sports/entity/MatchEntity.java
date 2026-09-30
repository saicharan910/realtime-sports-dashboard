package com.example.sports.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.sports.model.MatchStatus;

@Entity
@Table(name = "cricket_matches", indexes = {
        @Index(name = "idx_match_status", columnList = "status"),
        @Index(name = "idx_match_date", columnList = "match_date"),
        @Index(name = "idx_match_series", columnList = "series")
})
public class MatchEntity {

    @Id
    @Column(length = 100, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 150)
    private String teamA;

    @Column(nullable = false, length = 150)
    private String teamB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(columnDefinition = "TEXT")
    private String score;

    @Column(length = 255)
    private String series;

    @Column(name = "match_date")
    private LocalDate matchDate;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }

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

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
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

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}