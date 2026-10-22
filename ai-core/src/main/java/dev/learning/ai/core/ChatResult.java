package dev.learning.ai.core;

public record ChatResult(String provider, String model, String content, Usage usage, boolean simulated) {
    public record Usage(Long inputTokens, Long outputTokens, String source) {
        public static Usage unavailable() { return new Usage(null, null, "unavailable"); }
    }
}
