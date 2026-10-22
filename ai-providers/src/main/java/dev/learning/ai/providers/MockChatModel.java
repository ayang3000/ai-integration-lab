package dev.learning.ai.providers;

import dev.learning.ai.core.*;

public final class MockChatModel implements ChatModel {
    @Override public ChatResult chat(ChatRequest request) {
        return new ChatResult("mock", "mock-v1", "[MOCK] Integration path works. No model inference was performed.",
                ChatResult.Usage.unavailable(), true);
    }
}
