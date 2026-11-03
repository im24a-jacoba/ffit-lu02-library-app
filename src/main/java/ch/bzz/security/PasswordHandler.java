package ch.bzz.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Creates salts and password hashes and verifies them - and nothing else (SRP).
 * The plaintext password is never persisted, only the salt and the hash.
 */
public final class PasswordHandler {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    private PasswordHandler() {
        // utility class
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        md.update(salt);
        return md.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return true if the plaintext password results in the stored hash
     */
    public static boolean verifyPassword(String inputPassword, byte[] storedHash, byte[] storedSalt)
            throws NoSuchAlgorithmException {
        byte[] hashedInput = hashPassword(inputPassword, storedSalt);
        // isEqual instead of Arrays.equals: it always compares the full array, so the
        // duration of the comparison does not reveal how many bytes were correct.
        return MessageDigest.isEqual(hashedInput, storedHash);
    }

    public static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
