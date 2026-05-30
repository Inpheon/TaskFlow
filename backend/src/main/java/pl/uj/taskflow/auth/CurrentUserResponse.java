package pl.uj.taskflow.auth;

import java.util.UUID;

import pl.uj.taskflow.user.User;

public record CurrentUserResponse(UUID id, String email, String displayName) {

    static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
