package sorbonne.professional_website.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import sorbonne.professional_website.upload.StorageException;
import sorbonne.professional_website.upload.StorageFileNotFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "UPLOAD_TOO_LARGE",
                "Le fichier dépasse la taille maximale autorisée de 10 MB.",
                request,
                Map.of("maxFileSize", "10MB", "maxRequestSize", "12MB")
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipartException(
            MultipartException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_MULTIPART_REQUEST",
                "La requête d'upload est invalide ou le fichier envoyé n'est pas lisible.",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "La requête contient des données invalides.",
                request,
                Map.of("fields", fields)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonParseException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        String message = "Le corps de la requête n'est pas lisible ou le JSON est mal formé.";

        if (exception.getCause() instanceof InvalidFormatException invalidFormatException) {
            String fieldPath = invalidFormatException.getPath().stream()
                    .map(reference -> reference.getFieldName() != null
                            ? reference.getFieldName()
                            : String.valueOf(reference.getIndex()))
                    .collect(Collectors.joining("."));
            Object invalidValue = invalidFormatException.getValue();

            if (!fieldPath.isBlank()) details.put("field", fieldPath);
            if (invalidValue != null) details.put("rejectedValue", String.valueOf(invalidValue));
            if (invalidFormatException.getTargetType() != null) {
                details.put("expectedType", invalidFormatException.getTargetType().getSimpleName());
            }
        }

        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                message,
                request,
                details
        );
    }

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                safeMessage(exception, "Ressource introuvable."),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageFileNotFoundException(
            StorageFileNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "STORAGE_FILE_NOT_FOUND",
                safeMessage(exception, "Fichier introuvable."),
                request,
                Map.of()
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "BUSINESS_RULE_VIOLATION",
                safeMessage(exception, "La requête ne respecte pas une règle métier."),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(
            StorageException exception,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        LOGGER.error("Storage failure requestId={} path={}", requestId, request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "STORAGE_ERROR",
                "Le stockage du fichier a échoué.",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        LOGGER.error("Unexpected failure requestId={} path={}", requestId, request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Une erreur interne est survenue.",
                request,
                Map.of()
        );
    }

    private static ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, Object> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                requestId(request),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        if (attribute instanceof String value && !value.isBlank()) return value;

        String header = request.getHeader(RequestIdFilter.HEADER_NAME);
        return header == null || header.isBlank() ? "unavailable" : header;
    }

    private static String safeMessage(RuntimeException exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}
