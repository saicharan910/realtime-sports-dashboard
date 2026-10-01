package com.example.sports;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.entity.MatchEntity;
import com.example.sports.model.MatchStatus;
import com.example.sports.repository.MatchRepository;
import com.example.sports.service.MatchPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MatchPersistenceServiceTests {

    @Autowired
    private MatchPersistenceService persistenceService;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void preservesExistingScoreWhenIncomingScoreIsMissing() {
        MatchEntity existing = new MatchEntity();
        existing.setId("match-1");
        existing.setTeamA("India");
        existing.setTeamB("Sri Lanka");
        existing.setStatus(MatchStatus.LIVE);
        existing.setScore("145/4 - 142/8");
        existing.setSeries("Test Series");
        existing.setMatchDate(LocalDate.of(2026, 10, 1));
        matchRepository.save(existing);

        MatchResponseDTO incoming = new MatchResponseDTO(
                "match-1",
                "India",
                "Sri Lanka",
                MatchStatus.LIVE,
                "-",
                "Test Series",
                LocalDate.of(2026, 10, 1),
                null
        );

        persistenceService.saveIfChanged(incoming);

        MatchEntity saved = matchRepository.findById("match-1").orElseThrow();

        assertThat(saved.getScore()).isEqualTo("145/4 - 142/8");
    }

    @Test
    void savesNewMeaningfulScore() {
        MatchResponseDTO incoming = new MatchResponseDTO(
                "match-2",
                "Bangladesh",
                "Pakistan",
                MatchStatus.COMPLETED,
                "111/7 - 112/4",
                "T20 Asian Games",
                LocalDate.of(2026, 10, 1),
                null
        );

        persistenceService.saveIfChanged(incoming);

        MatchEntity saved = matchRepository.findById("match-2").orElseThrow();

        assertThat(saved.getScore()).isEqualTo("111/7 - 112/4");
        assertThat(saved.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }
}
