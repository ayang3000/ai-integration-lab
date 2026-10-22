package dev.learning.ai.core;

import java.util.List;

public record ChatRequest(String model, List<Message> messages) {
    public ChatRequest {
        if (model == null || model.isBlank() || model.length() > 80)
            throw new IllegalArgumentException("Invalid model alias");
        if (messages == null || messages.isEmpty() || messages.size() > 32)
            throw new IllegalArgumentException("Invalid message count");
        messages = List.copyOf(messages);
        if (messages.stream().mapToInt(m -> m.content().length()).sum() > 32000)
            throw new IllegalArgumentException("Input exceeds character budget");
    }
}
