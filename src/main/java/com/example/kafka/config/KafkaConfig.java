package com.example.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

@Configuration
public class KafkaConfig {

    /**
     * Kafka Transaction Manager for transactional publishing.
     * This enables @Transactional support for Kafka operations.
     */
    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    /**
     * Configure KafkaTemplate to work with transactions.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Topic creation beans - creates topics automatically if they don't exist

    @Bean
    public NewTopic stagingATopic() {
        return TopicBuilder.name("stagingA")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stagingBTopic() {
        return TopicBuilder.name("stagingB")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic publicATopic() {
        return TopicBuilder.name("publicA")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic publicBTopic() {
        return TopicBuilder.name("publicB")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
