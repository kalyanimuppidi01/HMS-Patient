package org.hms.patient.exception;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static class ApiError {
        private String code;
        private String message;
        private String correlationId;
        private String timestamp;

        public ApiError() {}

        public ApiError(String code, String message, String correlationId) {
            this.code = code;
            this.message = message;
            this.correlationId = correlationId;
            this.timestamp = Instant.now().toString();
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        String cid = UUID.randomUUID().toString();
        log.warn("NotFound: {} cid={} path={}", ex.getMessage(), cid, req.getRequestURI());
        ApiError e = new ApiError("NOT_FOUND", ex.getMessage(), cid);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        String cid = UUID.randomUUID().toString();
        log.warn("BadRequest: {} cid={} path={}", ex.getMessage(), cid, req.getRequestURI());
        ApiError e = new ApiError("BAD_REQUEST", ex.getMessage(), cid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handle(Exception ex, HttpServletRequest req) {
        String cid = UUID.randomUUID().toString();
        log.error("Error: {} cid={} path={}", ex.toString(), cid, req.getRequestURI(), ex);
        ApiError err = new ApiError("INTERNAL_ERROR", "Internal server error", cid);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
