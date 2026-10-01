
package com.example.sports.controller;

import com.example.sports.entity.StandingEntity;
import com.example.sports.repository.StandingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@CrossOrigin(origins = "*")
public class StandingController {

    private final StandingRepository standingRepository;

    public StandingController(StandingRepository standingRepository) {
        this.standingRepository = standingRepository;
    }

    @GetMapping("/{series}")
    public List<StandingEntity> getStandingsBySeries(@PathVariable String series) {
        return standingRepository.findByTournamentSeriesOrderByPointsDescNetRunRateDesc(series);
    }
}
