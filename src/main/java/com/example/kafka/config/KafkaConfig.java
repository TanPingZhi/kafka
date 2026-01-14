package com.example.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    @Primary
    public ProducerFactory<String, Object> producerFactory(KafkaProperties properties) {
        Map<String, Object> configs = properties.buildProducerProperties(null);
        
        // Use JsonSerializer directly for the main producer
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(
                configs, 
                null, // Key serializer (use default from configs)
                new JsonSerializer<>() // Value serializer for JSON objects
        );
        
        factory.setTransactionIdPrefix(properties.getProducer().getTransactionIdPrefix());
        return factory;
    }

    /**
     * Kafka Transaction Manager for transactional publishing.
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
