package com.example.kafka;

import com.example.kafka.service.MessagePublisherService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(partitions = 1, topics = { "stagingA", "stagingB", "publicA", "publicB" }, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1", "transaction.state.log.num.partitions=1" })
public class TransactionIntegrationTest {

    @Autowired
    private MessagePublisherService publisherService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    public void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // We need to trust the package for deserialization if we were using JsonDeserializer, but here we read as String to check content raw or json string
        // Actually the producer produces JSON.
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    public void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    public void testTransactionRollback() {
        // Arrange
        List<String> messages = List.of("Message 1", "FAIL", "Message 2");

        // Act & Assert
        assertThatThrownBy(() -> publisherService.publishToStagingQueues(messages))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("transaction rolled back");

        // Verification
        // Poll for records. Should be empty.
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        records.forEach(r -> System.out.println("Received: " + r.value() + " on topic " + r.topic()));
        assertThat(records.count()).isEqualTo(0);
    }

    @Test
    public void testTransactionCommit() {
         List<String> messages = List.of("Message 1", "Message 2");
         publisherService.publishToStagingQueues(messages);

         // We check stagingA. Since we send 2 messages, we expect 2 records.
         ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
         // Note: consumeFromAllEmbeddedTopics subscribes to all topics including stagingA, stagingB, etc.
         // publishToStagingQueues sends to stagingA AND stagingB. So 2 messages * 2 topics = 4 records total.

         assertThat(records.count()).isGreaterThanOrEqualTo(4);
         // We can be more specific if we filter by topic
    }
}
