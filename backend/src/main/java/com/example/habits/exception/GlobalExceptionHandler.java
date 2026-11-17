package com.example.habits.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.example.habits.security.RequestIdFilter;

import java.util.List;

/**
 * Renders every error as the single {@link ErrorResponse} envelope.
 *
 * <p>Hard rule: never include stack traces, exception class names, SQL, or any
 * other internal detail in {@code detail}. The {@code traceId} lets us match a
 * client-reported failure to server logs (see RequestIdFilter).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getType(), ex.getStatus(), ex.getMessage(), ex.getFieldErrors(), request);
    }

    /** Bean-validation failures on @Valid request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(),
                        defaultText(fe.getDefaultMessage())))
                .toList();
        return build(ErrorType.VALIDATION_ERROR, 400, "One or more fields are invalid",
                fieldErrors, request);
    }

    /** Bean-validation failures on @Validated query params / path variables. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(),
                        defaultText(fe.getDefaultMessage())))
                .toList();
        return build(ErrorType.VALIDATION_ERROR, 400, "One or more fields are invalid",
                fieldErrors, request);
    }

    /** Malformed JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return build(ErrorType.VALIDATION_ERROR, 400, "Malformed request body", null, request);
    }

    /** Missing required query/path parameters (e.g. analytics from/to). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = List.of(new ErrorResponse.FieldError(
                ex.getParameterName(), "required"));
        return build(ErrorType.VALIDATION_ERROR, 400, "One or more fields are invalid",
                fieldErrors, request);
    }

    /** Malformed UUID / date path variables, bad enum params, etc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(ErrorType.VALIDATION_ERROR, 400,
                "Invalid value for '" + ex.getName() + "'", null, request);
    }

    /** Spring Security authorization failures (CSRF, access rules). */
    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleSecurity(RuntimeException ex,
                                                        HttpServletRequest request) {
        if (ex instanceof AccessDeniedException) {
            return build(ErrorType.FORBIDDEN, 403, "Access denied", null, request);
        }
        return build(ErrorType.UNAUTHORIZED, 401, "Authentication required", null, request);
    }

    /** Last resort: log server-side with traceId, return an opaque 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = currentTraceId(request);
        log.error("Unhandled exception traceId={}", traceId, ex);
        return build(ErrorType.INTERNAL_ERROR, 500,
                "An unexpected error occurred. TraceId: " + traceId, null, request);
    }

    private ResponseEntity<ErrorResponse> build(ErrorType type, int status, String detail,
                                                List<ErrorResponse.FieldError> fieldErrors,
                                                HttpServletRequest request) {
        if (status >= 500) {
            log.error("{} -> {} {}", currentTraceId(request), status, type);
        } else {
            log.debug("{} -> {} {} ({})", currentTraceId(request), status, type, detail);
        }
        ErrorResponse body = new ErrorResponse(type, type.getDefaultTitle(), status,
                detail, fieldErrors, currentTraceId(request));
        return ResponseEntity.status(status).body(body);
    }

    private String currentTraceId(HttpServletRequest request) {
        Object attr = request != null ? request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)
                : null;
        return attr instanceof String s ? s : "unknown";
    }

    private String defaultText(String message) {
        return message == null ? "invalid value" : message;
    }
}
