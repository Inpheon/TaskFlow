package pl.uj.taskflow.auth;

public class InvalidCredentialsException extends RuntimeException {

    InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
