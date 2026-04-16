package com.kafka.notification_service.consume;

import com.kafka.notification_service.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserKafkaConsumerWithRecordAck {

    private final String USER_MESSAGE_TOPIC = "user_message";
    private final String DEAD_LETTER_USER_MESSAGE_TOPIC = "user_message.DLT";
    private final String USER_CREATED_EVENT_TOPIC = "user-created-event-topic";
    private final String DEAD_LETTER_USER_CREATED_EVENT_TOPIC = "user-created-event-topic.DLT";

    @KafkaListener(topics = "user-message",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeUserKafa1(String message) {

        log.info("Received: {}", message);

        try {
            // simulate failure
            if (message.contains("fail")) {
                throw new RuntimeException("Processing failed");
            }

            // success case
            log.info("Processed successfully: {}", message);


        } catch (Exception e) {

            log.error("Error processing message: {}", message);
            // ❗ VERY IMPORTANT
            throw e; // → triggers retry
        }
    }

    @KafkaListener(
            topics = "user-message.DLT",
            groupId = "dlt-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeFromDeadUserKafkaTopic(String message) {

        log.error("Received message from DLT: {}", message);
        try {
            log.info("Message processed: {}", message);
        } catch (Exception e) {
            log.error("Error processing message: {}", message);
            throw e;
        }
    }

    @KafkaListener(
            topics = DEAD_LETTER_USER_CREATED_EVENT_TOPIC,
            containerFactory = "userCreatedHandlerContainerFactory"
    )
    public void handleDeadUserCreatedEvents(UserCreatedEvent userCreatedEvent) {
        log.info("Received user event from user-created-event-topic.DLT : {}", userCreatedEvent);
        log.info("User created event processed from dead letter que: {}", userCreatedEvent);
    }

    @KafkaListener(
            topics = USER_CREATED_EVENT_TOPIC,
            containerFactory = "userCreatedHandlerContainerFactory"
    )
    public void handleUserCreatedEvents(UserCreatedEvent userCreatedEvent) {

    log.info("Received: {}", userCreatedEvent);
    if(userCreatedEvent.getEmail().contains("fail")){
        throw new RuntimeException("Failed to send the Email Due to.....actual reason goes here");
    }
    log.info("Notifications/Email sent successfully for the user creation on: {}", userCreatedEvent.getEmail());

    }


}
