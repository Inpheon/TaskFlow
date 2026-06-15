package pl.uj.taskflow.security;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user no longer exists");
    }
}
