package com.example.kafka.controller;

import com.example.kafka.dto.MessageRequest;
import com.example.kafka.service.MessagePublisherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Message Controller", description = "API for publishing messages to Kafka staging queues")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    
    private final MessagePublisherService publisherService;
    
    @Value("${pod.number}")
    private int podNumber;

    public MessageController(MessagePublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping
    @Operation(
        summary = "Publish messages to staging queues",
        description = "Publishes all provided strings to both stagingA and stagingB queues transactionally. " +
                      "If any message equals 'FAIL', the transaction will be rolled back and no messages will be published."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages published successfully"),
        @ApiResponse(responseCode = "500", description = "Transaction rolled back due to 'FAIL' message or other error")
    })
    public ResponseEntity<Map<String, Object>> publishMessages(@RequestBody MessageRequest request) {
        log.info("Received request to publish {} messages", request.getMessages().size());
        
        try {
            publisherService.publishToStagingQueues(request.getMessages());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully published " + request.getMessages().size() + " messages to staging queues",
                "count", request.getMessages().size()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to publish messages: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Transaction rolled back: " + e.getMessage(),
                "count", 0
            ));
        }
    }
    
    @GetMapping("/pod-number")
    @Operation(
        summary = "Get pod number",
        description = "Returns the dedicated pod number configured via environment variable POD_NUMBER"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pod number retrieved successfully")
    })
    public ResponseEntity<Map<String, Integer>> getPodNumber() {
        log.info("Returning pod number: {}", podNumber);
        return ResponseEntity.ok(Map.of("podNumber", podNumber));
    }
}
