package com.example.kafka.service;

import com.example.kafka.service.MessagePublisherService.MessagePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StagingToPublicConsumer {

    private static final Logger log = LoggerFactory.getLogger(StagingToPublicConsumer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StagingToPublicConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @KafkaListener(topics = "stagingA", groupId = "staging-to-public-group")
    public void consumeStagingA(MessagePayload payload) {
        log.info("Transferring message from stagingA to publicA: {}", payload.getContent());
        kafkaTemplate.send("publicA", payload);
    }

    @Transactional
    @KafkaListener(topics = "stagingB", groupId = "staging-to-public-group")
    public void consumeStagingB(MessagePayload payload) {
        log.info("Transferring message from stagingB to publicB: {}", payload.getContent());
        kafkaTemplate.send("publicB", payload);
    }
}
