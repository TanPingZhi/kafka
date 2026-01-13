package com.example.kafka.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Stream processors for transferring messages from staging to public queues.
 * 
 * These functions consume from staging queues and produce to public queues within
 * Kafka transactions, ensuring exactly-once semantics. If a message is consumed
 * from staging, it is guaranteed to be published to public (or both operations
 * are rolled back).
 * 
 * Uses byte[] for raw passthrough - avoids JSON parsing issues with nested/complex payloads.
 */
@Configuration
public class StagingToPublicProcessor {

    private static final Logger log = LoggerFactory.getLogger(StagingToPublicProcessor.class);

    /**
     * Processes messages from stagingA to publicA.
     * Runs within a Kafka transaction for guaranteed delivery.
     * Uses byte[] to pass through raw JSON without parsing.
     */
    @Bean
    public Function<byte[], byte[]> stagingAToPublicA() {
        return payload -> {
            log.info("Transferring message from stagingA to publicA ({} bytes)", payload.length);
            // Simply pass through raw bytes - the transaction ensures atomic consume + produce
            return payload;
        };
    }

    /**
     * Processes messages from stagingB to publicB.
     * Runs within a Kafka transaction for guaranteed delivery.
     * Uses byte[] to pass through raw JSON without parsing.
     */
    @Bean
    public Function<byte[], byte[]> stagingBToPublicB() {
        return payload -> {
            log.info("Transferring message from stagingB to publicB ({} bytes)", payload.length);
            // Simply pass through raw bytes - the transaction ensures atomic consume + produce
            return payload;
        };
    }
}
