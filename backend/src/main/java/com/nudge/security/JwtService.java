package com.nudge.security;

import com.nudge.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies the JWT access tokens.
 *
 * <p>Simple design: the token contains the user's email as the subject, so a
 * request carrying a valid token can be trusted without a database round trip
 * for signature verification alone (the filter still loads the user for the
 * SecurityContext).</p>
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(@Value("${nudge.jwt.secret}") String secret,
                      @Value("${nudge.jwt.expiration-minutes:720}") long expirationMinutes) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // HS256 needs 32 bytes of key material; 64 bytes lets jjwt use HS512.
            throw new IllegalStateException(
                    "nudge.jwt.secret must be at least 32 characters (64 or more is "
                            + "recommended so that HS512 is used). Set the NUDGE_JWT_SECRET "
                            + "environment variable or edit application.yml.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    /** Builds a signed token for the given user. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns the email stored in the token, or {@code null} when the token is
     * missing, malformed, unsigned by our key, or expired.
     */
    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
