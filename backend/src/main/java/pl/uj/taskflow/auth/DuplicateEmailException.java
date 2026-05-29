package pl.uj.taskflow.auth;

public class DuplicateEmailException extends RuntimeException {

    DuplicateEmailException(String email) {
        super("User with email already exists: " + email);
    }
}
