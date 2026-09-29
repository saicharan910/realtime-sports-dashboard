
package com.example.sports.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_standings")
public class StandingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tournamentSeries;
    private String teamName;
    private int played = 0;
    private int won = 0;
    private int lost = 0;
    private int points = 0;
    private double netRunRate = 0.0;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTournamentSeries() { return tournamentSeries; }
    public void setTournamentSeries(String tournamentSeries) { this.tournamentSeries = tournamentSeries; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public int getPlayed() { return played; }
    public void setPlayed(int played) { this.played = played; }
    public int getWon() { return won; }
    public void setWon(int won) { this.won = won; }
    public int getLost() { return lost; }
    public void setLost(int lost) { this.lost = lost; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public double getNetRunRate() { return netRunRate; }
    public void setNetRunRate(double netRunRate) { this.netRunRate = netRunRate; }
}