package ch.bzz.security;

import ch.bzz.Config;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens - and nothing else (SRP).
 * The signature makes sure that nobody can create a valid token without the secret.
 */
public final class JwtHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtHandler.class);

    private static final String SECRET_PROPERTY = "jwt.secret";
    private static final int MINIMUM_SECRET_LENGTH = 32;
    private static final long VALIDITY_MILLIS = 3_600_000;
    private static final String USER_ID_CLAIM = "userId";

    private static final SecretKey JWT_KEY = loadKey();

    private JwtHandler() {
        // utility class
    }

    public static String createJwt(String subject, Integer userId) {
        Date currentTime = new Date();
        Date expirationTime = new Date(currentTime.getTime() + VALIDITY_MILLIS);

        return Jwts.builder()
                .subject(subject)
                .claim(USER_ID_CLAIM, userId)
                .issuedAt(currentTime)
                .expiration(expirationTime)
                .signWith(JWT_KEY)
                .compact();
    }

    /**
     * Verifies the signature and the expiration date of the token.
     *
     * @return the payload of the token
     * @throws JwtException if the token is invalid, expired or was signed with another secret
     */
    public static Claims parseJwt(String token) {
        return Jwts.parser()
                .verifyWith(JWT_KEY) // This includes the verification
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * @return the id of the user the token was issued for
     * @throws JwtException if the token is invalid
     */
    public static Integer getUserId(String token) {
        Claims claims = parseJwt(token);
        Integer userId = claims.get(USER_ID_CLAIM, Integer.class);
        if (userId == null || userId <= 0 || claims.getExpiration() == null) {
            throw new JwtException("Token requires a user id and expiration");
        }
        return userId;
    }

    /**
     * Reads the secret out of config.properties. If it is missing or too short a random
     * key is used instead, so the API still starts - but every restart invalidates all
     * tokens issued before, which is why a real deployment must configure the secret.
     */
    private static SecretKey loadKey() {
        String secret = Config.get(SECRET_PROPERTY);

        if (secret == null || secret.length() < MINIMUM_SECRET_LENGTH) {
            log.warn("'{}' is missing or shorter than {} characters in config.properties. "
                            + "A random key is generated, all tokens become invalid on the next restart.",
                    SECRET_PROPERTY, MINIMUM_SECRET_LENGTH);
            return Jwts.SIG.HS256.key().build();
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
