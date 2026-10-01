package com.example.sports.service;

import com.example.sports.event.MatchUpdatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final String websocketTopic;

    public MatchUpdatePublisher(
            SimpMessagingTemplate messagingTemplate,
            @Value("${sportsdata.websocket-topic}") String websocketTopic) {

        this.messagingTemplate = messagingTemplate;
        this.websocketTopic = websocketTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MatchUpdatedEvent event) {

        messagingTemplate.convertAndSend(
                websocketTopic,
                event.match()
        );
    }
}