package com.example.sports.model;

public class MatchData {
    private String teamA;
    private String teamB;
    private int scoreA;
    private int scoreB;
    private int overs;

    public MatchData(String teamA, String teamB, int scoreA, int scoreB, int overs) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.overs = overs;
    }

    // Getters are required for Jackson to convert to JSON
    public String getTeamA() {
        return teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public int getOvers() {
        return overs;
    }
}