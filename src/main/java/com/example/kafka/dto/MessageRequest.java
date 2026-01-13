package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request containing a list of messages to publish")
public class MessageRequest {

    @Schema(description = "List of string messages to publish to Kafka staging queues", 
            example = "[\"message1\", \"message2\", \"message3\"]")
    private List<String> messages;

    public MessageRequest() {
    }

    public MessageRequest(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
