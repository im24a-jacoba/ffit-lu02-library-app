package ch.bzz.api;

import ch.bzz.db.UserPersistor;
import ch.bzz.model.User;
import ch.bzz.security.JwtHandler;
import ch.bzz.security.PasswordHandler;
import io.javalin.http.Context;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * HTTP endpoints for login and password changes - and nothing else (SRP).
 */
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_TOKEN = "Invalid or missing token";

    private final UserPersistor userPersistor;

    public AuthController(UserPersistor userPersistor) {
        this.userPersistor = userPersistor;
    }

    /**
     * POST /auth/login with the body {"email": "...", "password": "..."}
     * Answers with {"token": "&lt;jwt&gt;"} or 401.
     */
    public void login(Context ctx) {
        var json = ctx.bodyValidator(Map.class)
                .check(m -> m.get("email") instanceof String value && !value.isBlank(), "email is required")
                .check(m -> m.get("password") instanceof String value && !value.isBlank(), "password is required")
                .get();

        String inputEmail = String.valueOf(json.get("email"));
        String inputPassword = String.valueOf(json.get("password"));

        User user = userPersistor.findByEmail(inputEmail);

        if (user != null && passwordMatches(inputPassword, user)) {
            String jwt = JwtHandler.createJwt(inputEmail, user.getId());
            log.info("User '{}' logged in", inputEmail);
            ctx.json(Map.of("token", jwt));
            return;
        }

        // The same message for an unknown user and for a wrong password - otherwise an
        // attacker could find out which email addresses exist.
        log.warn("Failed login attempt for '{}'", inputEmail);
        ctx.status(401).json(Map.of("error", INVALID_CREDENTIALS));
    }

    /**
     * PUT /auth/change-password with the header "Authorization: Bearer &lt;jwt&gt;"
     * and the body {"oldPassword": "...", "newPassword": "..."}.
     */
    public void changePassword(Context ctx) {
        Integer userId;
        try {
            userId = JwtHandler.getUserId(readBearerToken(ctx));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected a change-password request: {}", e.getMessage());
            ctx.status(401).json(Map.of("error", INVALID_TOKEN));
            return;
        }

        var json = ctx.bodyValidator(Map.class)
                .check(m -> m.get("oldPassword") instanceof String value && !value.isBlank(), "oldPassword is required")
                .check(m -> m.get("newPassword") instanceof String value && !value.isBlank(), "newPassword is required")
                .get();

        String oldPassword = String.valueOf(json.get("oldPassword"));
        String newPassword = String.valueOf(json.get("newPassword"));

        if (newPassword.isBlank()) {
            ctx.status(400).json(Map.of("error", "newPassword must not be empty"));
            return;
        }

        User user = userPersistor.findById(userId);

        if (user == null || !passwordMatches(oldPassword, user)) {
            log.warn("Failed password change for user id {}", userId);
            ctx.status(401).json(Map.of("error", INVALID_CREDENTIALS));
            return;
        }

        try {
            applyNewPassword(user, newPassword);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm is not available, the password of user id {} stays unchanged", userId, e);
            ctx.status(500).json(Map.of("error", "Password could not be changed"));
            return;
        }

        if (userPersistor.trySave(user)) {
            log.info("User '{}' changed the password", user.getEmail());
            ctx.json(Map.of("message", "Password changed successfully"));
        } else {
            ctx.status(500).json(Map.of("error", "Password could not be changed"));
        }
    }

    private boolean passwordMatches(String password, User user) {
        try {
            byte[] storedSalt = PasswordHandler.decode(user.getPasswordSalt());
            byte[] storedHash = PasswordHandler.decode(user.getPasswordHash());
            return PasswordHandler.verifyPassword(password, storedHash, storedSalt);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            log.error("Could not verify the password of user '{}'", user.getEmail(), e);
            return false;
        }
    }

    /**
     * A password change also gets a new salt, so the old and the new hash cannot be
     * compared with each other.
     */
    private void applyNewPassword(User user, String newPassword) throws NoSuchAlgorithmException {
        byte[] salt = PasswordHandler.generateSalt();
        byte[] hash = PasswordHandler.hashPassword(newPassword, salt);

        user.setPasswordSalt(PasswordHandler.encode(salt));
        user.setPasswordHash(PasswordHandler.encode(hash));
    }

    private String readBearerToken(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }

        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
