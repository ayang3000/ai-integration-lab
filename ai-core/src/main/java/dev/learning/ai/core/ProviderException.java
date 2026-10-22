package dev.learning.ai.core;

/** Deliberately contains no provider payload, URL, prompt or original exception message. */
public final class ProviderException extends RuntimeException {
    public ProviderException() { super("Model provider failed"); }
}
