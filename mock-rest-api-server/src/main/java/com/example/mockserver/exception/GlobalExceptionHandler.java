package com.example.mockserver.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage(),
                        "traceId", traceId(request)
                ));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "INVALID_REQUEST",
                        "message", ex.getMessage() == null ? "Invalid request" : ex.getMessage(),
                        "traceId", traceId(request)
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_ERROR",
                        "message", "Unexpected server error",
                        "traceId", traceId(request)
                ));
    }

    private String traceId(HttpServletRequest request) {
        Object existing = request.getAttribute("traceId");
        if (existing instanceof String value) {
            return value;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute("traceId", generated);
        return generated;
    }
}
