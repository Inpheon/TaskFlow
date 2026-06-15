package pl.uj.taskflow.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.security.AuthenticatedUser;
import pl.uj.taskflow.security.AuthenticatedUserNotFoundException;
import pl.uj.taskflow.security.JwtService;
import pl.uj.taskflow.user.User;
import pl.uj.taskflow.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthSessionResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = userRepository.save(new User(
            email,
            passwordEncoder.encode(request.password()),
            request.displayName().trim()
        ));

        return session(user);
    }

    @Transactional(readOnly = true)
    public AuthSessionResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
            .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        return session(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(AuthenticatedUser user) {
        return userRepository.findById(user.id())
            .map(CurrentUserResponse::from)
            .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private AuthSessionResponse session(User user) {
        return AuthSessionResponse.bearer(jwtService.createToken(user), CurrentUserResponse.from(user));
    }
}
