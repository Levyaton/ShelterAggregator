package cz.levy.pet.shelter.aggregator.error;

import java.util.NoSuchElementException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class RestErrorHandler {

  @Data
  @AllArgsConstructor
  public static class ErrorResponse {
    private String code;
    private String message;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(MethodArgumentNotValidException ex) {
    var fieldError = ex.getBindingResult().getFieldErrors().getFirst();

    return createErrorResponseEntity(
        HttpStatus.BAD_REQUEST,
        "Invalid request parameters: "
            + ex.getParameter().getParameterName()
            + "."
            + fieldError.getField()
            + " "
            + fieldError.getDefaultMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return createErrorResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
    return createErrorResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
    return createErrorResponseEntity(HttpStatus.CONFLICT, ex.getMessage());
  }

  private ResponseEntity<ErrorResponse> createErrorResponseEntity(
      HttpStatus status, String detail) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorResponse(status.name(), detail));
  }

  public static class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
      super(message);
    }
  }
}
