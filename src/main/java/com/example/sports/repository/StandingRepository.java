
package com.example.sports.repository;

import com.example.sports.entity.StandingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<StandingEntity, Long> {
    Optional<StandingEntity> findByTournamentSeriesAndTeamName(String tournamentSeries, String teamName);
    List<StandingEntity> findByTournamentSeriesOrderByPointsDescNetRunRateDesc(String tournamentSeries);
}