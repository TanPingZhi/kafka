package com.example.kafka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessagePublisherService {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisherService.class);
    
    private static final String STAGING_A_TOPIC = "stagingA";
    private static final String STAGING_B_TOPIC = "stagingB";
    private static final String FAIL_TRIGGER = "FAIL";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MessagePublisherService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes messages to both staging queues transactionally.
     * If any message equals "FAIL", throws RuntimeException to trigger rollback.
     * 
     * @param messages List of string messages to publish
     * @throws RuntimeException if "FAIL" message is encountered
     */
    @Transactional("kafkaTransactionManager")
    public void publishToStagingQueues(List<String> messages) {
        log.info("Starting transactional publish of {} messages to staging queues", messages.size());
        
        for (String message : messages) {
            // Check for fail trigger BEFORE publishing
            if (FAIL_TRIGGER.equals(message)) {
                log.error("Encountered FAIL trigger - rolling back transaction");
                throw new RuntimeException("Encountered 'FAIL' message - transaction rolled back");
            }
            
            // Create a message wrapper for JSON serialization
            MessagePayload payload = new MessagePayload(message);
            
            // Publish to stagingA
            log.debug("Publishing message to {}: {}", STAGING_A_TOPIC, message);
            kafkaTemplate.send(STAGING_A_TOPIC, payload);
            
            // Publish to stagingB
            log.debug("Publishing message to {}: {}", STAGING_B_TOPIC, message);
            kafkaTemplate.send(STAGING_B_TOPIC, payload);
        }
        
        log.info("Successfully queued {} messages to both staging queues", messages.size());
    }
    
    /**
     * Simple payload wrapper for JSON serialization
     */
    public static class MessagePayload {
        private String content;
        private long timestamp;
        
        public MessagePayload() {
        }
        
        public MessagePayload(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
