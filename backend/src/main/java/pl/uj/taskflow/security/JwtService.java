package pl.uj.taskflow.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.uj.taskflow.user.User;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secret;
    private final Duration expiration;

    JwtService(
        @Value("${taskflow.jwt.secret}") String secret,
        @Value("${taskflow.jwt.expiration}") Duration expiration
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("taskflow.jwt.secret must be at least 32 bytes for HS256");
        }
        this.expiration = expiration;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("displayName", user.getDisplayName())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(expiration)))
            .build();

        try {
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(new MACSigner(secret));
            return signedJwt.serialize();
        } catch (com.nimbusds.jose.JOSEException exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    public Optional<AuthenticatedUser> parseToken(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!signedJwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) {
                return Optional.empty();
            }

            return Optional.of(new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.getStringClaim("email"),
                claims.getStringClaim("displayName")
            ));
        } catch (java.text.ParseException | com.nimbusds.jose.JOSEException | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
