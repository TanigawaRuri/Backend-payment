package com.tanigawa.rewardplatform.notification;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publish(RewardClaimedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, String.valueOf(event.userId()), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("kafka send failed historyId={}", event.eventId(), ex);
                    } else {
                        var m = result.getRecordMetadata();
                        log.info("kafka sent partition={} offset={} historyId={}",
                                m.partition(), m.offset(), event.eventId());
                    }
                });
    }
}