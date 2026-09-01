package com.basilico.backend.common.error;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
		return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException exception) {
		return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
		return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(ServiceUnavailableException exception) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
				.collect(Collectors.joining("; "));

		if (message.isBlank()) {
			message = "Request validation failed";
		}

		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request body is invalid");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
		return error(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with existing data");
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
		return ResponseEntity
				.status(status)
				.body(new ApiErrorResponse(status.value(), code, message));
	}
}
