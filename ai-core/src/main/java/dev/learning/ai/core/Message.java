package dev.learning.ai.core;

public record Message(String role, String content) {
    public Message {
        if (!"user".equals(role)) throw new IllegalArgumentException("Only user messages are supported");
        if (content == null || content.isBlank() || content.length() > 16000)
            throw new IllegalArgumentException("Invalid message content");
    }
}
