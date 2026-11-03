package ch.bzz;

import ch.bzz.security.JwtHandler;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtHandlerTest {
    private final javax.crypto.SecretKey key = Keys.hmacShaKeyFor(
            Config.get("jwt.secret").getBytes(StandardCharsets.UTF_8));

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder().claim("userId", 1)
                .expiration(new Date(System.currentTimeMillis() - 60_000)).signWith(key).compact();
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> JwtHandler.getUserId(token));
    }

    @Test
    void tokenWithoutUserOrExpirationIsRejected() {
        String noUser = Jwts.builder().expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key).compact();
        String noExpiry = Jwts.builder().claim("userId", 1).signWith(key).compact();
        assertThrows(JwtException.class, () -> JwtHandler.getUserId(noUser));
        assertThrows(JwtException.class, () -> JwtHandler.getUserId(noExpiry));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = Jwts.builder().claim("userId", 1)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        assertThrows(JwtException.class, () -> JwtHandler.getUserId(token));
    }
}
