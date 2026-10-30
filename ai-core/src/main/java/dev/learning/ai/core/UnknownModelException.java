package dev.learning.ai.core;

public final class UnknownModelException extends RuntimeException {
    public UnknownModelException() { super("Unknown model alias"); }
}
