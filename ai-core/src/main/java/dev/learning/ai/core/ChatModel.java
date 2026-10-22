package dev.learning.ai.core;

@FunctionalInterface
public interface ChatModel {
    ChatResult chat(ChatRequest request);
}
