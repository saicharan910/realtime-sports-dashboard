package com.example.sports;

import com.example.sports.dto.MatchResponseDTO;
import com.example.sports.event.MatchUpdatedEvent;
import com.example.sports.model.MatchStatus;
import com.example.sports.service.MatchUpdatePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MatchUpdatePublisherTests {

    @Test
    void publishesMatchToConfiguredTopic() {

        SimpMessagingTemplate messagingTemplate =
                mock(SimpMessagingTemplate.class);

        MatchUpdatePublisher publisher =
                new MatchUpdatePublisher(
                        messagingTemplate,
                        "/topic/scores"
                );

        MatchResponseDTO match = new MatchResponseDTO(
                "match-1",
                "India",
                "Australia",
                MatchStatus.LIVE,
                "101/2 - 99/3",
                "Test Series",
                LocalDate.of(2026, 9, 30),
                null
        );

        publisher.publish(new MatchUpdatedEvent(match));

        verify(messagingTemplate)
                .convertAndSend("/topic/scores", match);
    }

    @Test
    void publishMethodIsConfiguredForAfterCommit() throws Exception {

        var method = MatchUpdatePublisher.class
                .getDeclaredMethod(
                        "publish",
                        MatchUpdatedEvent.class
                );

        var annotation =
                method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase())
                .isEqualTo(
                        org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT
                );
    }
}