package dev.learning.ai.launcher;

import dev.learning.ai.core.ProviderException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
final class ErrorHandler {
    record ErrorBody(String requestId, String code, String message) {}

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    ResponseEntity<ErrorBody> invalid(Exception ignored, HttpServletRequest request) {
        return error(400, "INVALID_REQUEST", "Invalid chat request", request);
    }
    @ExceptionHandler(ChatController.UnknownModelException.class)
    ResponseEntity<ErrorBody> unknown(HttpServletRequest request) {
        return error(404, "UNKNOWN_MODEL", "Unknown model alias", request);
    }
    @ExceptionHandler(ProviderException.class)
    ResponseEntity<ErrorBody> provider(HttpServletRequest request) {
        return error(502, "PROVIDER_FAILURE", "Model provider unavailable", request);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception failure, HttpServletRequest request) {
        if (failure instanceof org.springframework.web.ErrorResponse expected) {
            return error(expected.getStatusCode().value(), "HTTP_ERROR", "Unsupported HTTP request", request);
        }
        return error(500, "INTERNAL_ERROR", "Request failed", request);
    }
    private ResponseEntity<ErrorBody> error(int status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorBody(
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE), code, message));
    }
}
