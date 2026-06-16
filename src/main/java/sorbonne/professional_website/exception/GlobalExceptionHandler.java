package sorbonne.professional_website.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.put("error", "Upload too large");
        response.put("message", "Le fichier dépasse la taille maximale autorisée de 10 MB.");
        response.put("maxFileSize", "10MB");
        response.put("maxRequestSize", "12MB");

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Invalid multipart request");
        response.put("message", "La requête d'upload est invalide ou le fichier envoyé n'est pas lisible.");

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation failed");
        response.put("messages", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseException(
            HttpMessageNotReadableException exception
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Malformed JSON Request");

        String detailMessage = "Le corps de la requête n'est pas lisible ou le JSON est mal formé.";

        if (exception.getCause() instanceof InvalidFormatException ife) {
            String path = ife.getPath().stream()
                    .map(ref -> (ref.getFieldName() != null) ? ref.getFieldName() : String.valueOf(ref.getIndex()))
                    .collect(Collectors.joining("."));

            String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
            String targetType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "inconnu";

            detailMessage = String.format("Le champ '%s' a reçu une valeur invalide ('%s'). Type attendu : %s.",
                    path, invalidValue, targetType);

            response.put("field", path);
            response.put("rejectedValue", invalidValue);
        } else if (exception.getMessage() != null) {
            detailMessage = exception.getMessage();
        }

        response.put("message", detailMessage);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException exception
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Resource not found");
        response.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessException(RuntimeException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Business rule violation");
        response.put("message", exception.getMessage());

        return ResponseEntity.badRequest().body(response);
    }
}
