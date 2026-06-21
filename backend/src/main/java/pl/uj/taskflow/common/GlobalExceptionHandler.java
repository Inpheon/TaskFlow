package pl.uj.taskflow.common;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pl.uj.taskflow.auth.DuplicateEmailException;
import pl.uj.taskflow.auth.InvalidCredentialsException;
import pl.uj.taskflow.note.TaskNoteNotFoundException;
import pl.uj.taskflow.project.ProjectNotFoundException;
import pl.uj.taskflow.security.AuthenticatedUserNotFoundException;
import pl.uj.taskflow.task.InvalidTaskListQueryException;
import pl.uj.taskflow.task.InvalidTaskPositionException;
import pl.uj.taskflow.task.TaskNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Request validation failed");

        return error(HttpStatus.BAD_REQUEST, "Validation error", message, request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    ResponseEntity<ApiError> handleAuthenticatedUserNotFound(
        AuthenticatedUserNotFoundException exception,
        HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    ResponseEntity<ApiError> handleProjectNotFound(ProjectNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request);
    }

    @ExceptionHandler(TaskNoteNotFoundException.class)
    ResponseEntity<ApiError> handleTaskNoteNotFound(TaskNoteNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidTaskPositionException.class)
    ResponseEntity<ApiError> handleInvalidTaskPosition(
        InvalidTaskPositionException exception,
        HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Validation error", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidTaskListQueryException.class)
    ResponseEntity<ApiError> handleInvalidTaskListQuery(
        InvalidTaskListQueryException exception,
        HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Validation error", exception.getMessage(), request);
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleInvalidRequestParameters(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Validation error", "Request parameters are invalid", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Validation error", "Request body is invalid", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity
            .status(status)
            .body(new ApiError(status.value(), error, message, request.getRequestURI(), Instant.now()));
    }
}
