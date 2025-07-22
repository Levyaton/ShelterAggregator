package cz.levy.pet.shelter.aggregator.error;

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

  private ResponseEntity<ErrorResponse> createErrorResponseEntity(
      HttpStatus status, String detail) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorResponse(status.name(), detail));
  }
}
