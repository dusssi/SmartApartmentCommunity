package com.apartment.community.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing and verification for local account credentials.
 *
 * <p>Scheme: PBKDF2 (RFC 8018) with HMAC-SHA-256, a unique 16-byte {@link SecureRandom} salt
 * per account, 210,000 iterations and a 32-byte derived key. Every encoded credential is
 * self-describing so parameters can be raised later without breaking stored records:
 *
 * <pre>pbkdf2-sha256$&lt;iterations&gt;$&lt;saltHex&gt;$&lt;hashHex&gt;</pre>
 *
 * <p>{@code PBKDF2WithHmacSHA256} is only guaranteed on Android from API 26, so on API 24–25
 * devices the hasher falls back to {@code PBKDF2WithHmacSHA1} (210,000 iterations, same salt and
 * key sizes) and records the algorithm in the prefix ({@code pbkdf2-sha1$…}). Verification always
 * follows the stored algorithm and iteration count.
 *
 * <p>Hex encoding is used instead of Base64 because {@code java.util.Base64} requires API 26 and
 * {@code android.util.Base64} would make this class untestable on a plain JVM.
 *
 * <p>All comparisons run through {@link MessageDigest#isEqual(byte[], byte[])}, which is
 * constant-time for equal-length inputs; derived keys are fixed-length, so verification does not
 * leak hash bytes through timing.
 *
 * <p>Legacy credentials written by earlier builds (unsalted single-round SHA-256 hex, 64 chars)
 * are recognised by {@link #isLegacyHash(String)} and can be verified once via
 * {@link #verify(String, String)} so existing accounts are not locked out. Callers must treat a
 * successful legacy verification as "needs upgrade" ({@link #needsUpgrade(String)}) and re-hash
 * with {@link #hash(String)} immediately. Unknown, empty or malformed credentials never verify —
 * in particular a stored credential that is absent is never "adopted" from the next entered
 * password; account recovery must go through an explicit user-confirmed flow.
 *
 * <p>Pure Java (no Android dependencies) so it is covered by JVM unit tests.
 */
public final class PasswordHasher {

    /** PBKDF2 iteration count. Embedded in every encoded credential. */
    public static final int ITERATIONS = 210_000;
    /** Sanity ceiling when reading iterations from a stored credential. */
    public static final int MAX_STORED_ITERATIONS = 10_000_000;

    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;
    private static final int LEGACY_SHA256_HEX_LENGTH = 64;

    public static final String PREFIX_SHA256 = "pbkdf2-sha256";
    public static final String PREFIX_SHA1 = "pbkdf2-sha1";
    private static final String ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256";
    private static final String ALGORITHM_SHA1 = "PBKDF2WithHmacSHA1";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordHasher() { }

    /**
     * Hashes a raw password into the self-describing PBKDF2 format with a fresh random salt.
     * Prefers HMAC-SHA-256 and falls back to HMAC-SHA-1 only where the platform lacks it
     * (Android API 24–25).
     *
     * @throws IllegalArgumentException if the password is null or empty
     * @throws IllegalStateException    if no PBKDF2 algorithm is available at all
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        try {
            byte[] key = pbkdf2(ALGORITHM_SHA256, rawPassword, salt, ITERATIONS, KEY_BYTES);
            return encode(PREFIX_SHA256, ITERATIONS, salt, key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException missingSha256Variant) {
            // PBKDF2WithHmacSHA256 is only guaranteed from Android API 26; on API 24–25 the
            // SHA-1 variant is used and recorded in the credential prefix.
            try {
                byte[] key = pbkdf2(ALGORITHM_SHA1, rawPassword, salt, ITERATIONS, KEY_BYTES);
                return encode(PREFIX_SHA1, ITERATIONS, salt, key);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new IllegalStateException("No PBKDF2 algorithm available on this platform", e);
            }
        }
    }

    /** True when the credential uses the current PBKDF2 format with parseable parameters. */
    public static boolean isPbkdf2(String encoded) {
        if (encoded == null) return false;
        String[] parts = encoded.split("\\$");
        if (parts.length != 4) return false;
        if (!PREFIX_SHA256.equals(parts[0]) && !PREFIX_SHA1.equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            return iterations > 0 && iterations <= MAX_STORED_ITERATIONS
                    && fromHex(parts[2]) != null && fromHex(parts[3]) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * True when the credential is a legacy unsalted SHA-256 hex digest written by earlier builds.
     * Legacy credentials verify once but must be upgraded to PBKDF2 immediately.
     */
    public static boolean isLegacyHash(String encoded) {
        if (encoded == null || encoded.length() != LEGACY_SHA256_HEX_LENGTH) return false;
        for (int i = 0; i < encoded.length(); i++) {
            char c = Character.toLowerCase(encoded.charAt(i));
            if (c < '0' || (c > '9' && c < 'a') || c > 'f') return false;
        }
        return true;
    }

    /** True when the credential is present in a recognized (current or legacy) format. */
    public static boolean hasRecognizedFormat(String encoded) {
        return isPbkdf2(encoded) || isLegacyHash(encoded);
    }

    /**
     * True when a recognized credential should be re-hashed with {@link #hash(String)}
     * (anything that is not current PBKDF2, including legacy SHA-256).
     */
    public static boolean needsUpgrade(String encoded) {
        return encoded != null && !encoded.isEmpty() && !isPbkdf2(encoded);
    }

    /**
     * Verifies a raw password against a stored credential (PBKDF2 or legacy SHA-256 hex),
     * in constant time for the digest comparison. Never throws: null, empty, malformed or
     * unknown-format credentials simply fail. A missing credential can never be "adopted" —
     * verification requires a stored value in a recognized format.
     */
    public static boolean verify(String rawPassword, String storedCredential) {
        if (rawPassword == null || rawPassword.isEmpty() || storedCredential == null
                || storedCredential.isEmpty()) {
            return false;
        }
        try {
            if (isPbkdf2(storedCredential)) {
                String[] parts = storedCredential.split("\\$");
                int iterations = Integer.parseInt(parts[1]);
                if (iterations <= 0 || iterations > MAX_STORED_ITERATIONS) return false;
                byte[] salt = fromHex(parts[2]);
                byte[] expected = fromHex(parts[3]);
                if (salt == null || expected == null) return false;
                String algorithm = PREFIX_SHA256.equals(parts[0]) ? ALGORITHM_SHA256 : ALGORITHM_SHA1;
                byte[] candidate = pbkdf2(algorithm, rawPassword, salt, iterations, expected.length);
                return MessageDigest.isEqual(expected, candidate);
            }
            if (isLegacyHash(storedCredential)) {
                byte[] expected = storedCredential.toLowerCase(Locale.US)
                        .getBytes(StandardCharsets.UTF_8);
                byte[] candidate = sha256Hex(rawPassword).getBytes(StandardCharsets.UTF_8);
                return MessageDigest.isEqual(expected, candidate);
            }
            return false;
        } catch (RuntimeException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Fail safe: any unexpected problem means "not authenticated".
            return false;
        }
    }

    private static String encode(String prefix, int iterations, byte[] salt, byte[] key) {
        return prefix + "$" + iterations + "$" + toHex(salt) + "$" + toHex(key);
    }

    private static byte[] pbkdf2(String algorithm, String password, byte[] salt,
                                 int iterations, int keyLengthBytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // PBEKeySpec takes char[]; the derived key length is expressed in bits.
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBytes * 8);
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** Legacy single-round SHA-256 hex, retained ONLY to verify pre-PBKDF2 credentials once. */
    private static String sha256Hex(String raw) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return toHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    static byte[] fromHex(String hex) {
        if (hex == null || hex.isEmpty() || (hex.length() % 2) != 0) return null;
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) return null;
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
