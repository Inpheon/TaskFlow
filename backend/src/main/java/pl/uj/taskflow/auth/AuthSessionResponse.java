package pl.uj.taskflow.auth;

public record AuthSessionResponse(String accessToken, String tokenType, CurrentUserResponse user) {

    static AuthSessionResponse bearer(String accessToken, CurrentUserResponse user) {
        return new AuthSessionResponse(accessToken, "Bearer", user);
    }
}
