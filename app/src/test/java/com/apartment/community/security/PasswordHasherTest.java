package com.apartment.community.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PasswordHasher} — P0 fix #1 (salted, iterated password hashing) and
 * part of P0 fix #2 (an account without a stored credential must never "verify").
 */
public class PasswordHasherTest {

    @Test
    public void hash_producesCurrentPbkdf2Format() {
        String encoded = PasswordHasher.hash("correct horse battery staple");
        assertTrue("expected pbkdf2-sha256 or pbkdf2-sha1 prefix: " + encoded,
                encoded.startsWith(PasswordHasher.PREFIX_SHA256 + "$")
                        || encoded.startsWith(PasswordHasher.PREFIX_SHA1 + "$"));
        assertTrue(PasswordHasher.isPbkdf2(encoded));
        assertFalse(PasswordHasher.needsUpgrade(encoded));
    }

    @Test
    public void hash_recordsConfiguredIterationCount() {
        String encoded = PasswordHasher.hash("hunter22");
        String[] parts = encoded.split("\\$");
        assertEquals(4, parts.length);
        assertEquals(PasswordHasher.ITERATIONS, Integer.parseInt(parts[1]));
    }

    @Test
    public void hash_usesUniqueRandomSaltPerCall() {
        String a = PasswordHasher.hash("same-password");
        String b = PasswordHasher.hash("same-password");
        assertNotEquals("two hashes of the same password must differ (unique salts)", a, b);
        String saltA = a.split("\\$")[2];
        String saltB = b.split("\\$")[2];
        assertNotEquals(saltA, saltB);
    }

    @Test
    public void verify_acceptsTheCorrectPassword() {
        String encoded = PasswordHasher.hash("S3cur3-Pass!");
        assertTrue(PasswordHasher.verify("S3cur3-Pass!", encoded));
    }

    @Test
    public void verify_rejectsAnIncorrectPassword() {
        String encoded = PasswordHasher.hash("S3cur3-Pass!");
        assertFalse(PasswordHasher.verify("wrong-password", encoded));
        assertFalse(PasswordHasher.verify("s3cur3-pass!", encoded)); // case sensitive
    }

    @Test
    public void verify_rejectsEmptyOrNullInputs() {
        String encoded = PasswordHasher.hash("abcdef");
        assertFalse(PasswordHasher.verify("", encoded));
        assertFalse(PasswordHasher.verify(null, encoded));
        assertFalse(PasswordHasher.verify("abcdef", ""));
        assertFalse(PasswordHasher.verify("abcdef", null));
    }

    /**
     * P0 fix #2: a missing/blank stored credential must NEVER verify against any entered
     * password — this is what makes "first password adoption" impossible once PasswordHasher
     * is used as the sole verification path.
     */
    @Test
    public void verify_neverAdoptsAPasswordWhenNoCredentialIsStored() {
        assertFalse(PasswordHasher.verify("any-password-at-all", ""));
        assertFalse(PasswordHasher.verify("any-password-at-all", null));
        assertFalse(PasswordHasher.verify("", ""));
    }

    @Test
    public void verify_rejectsGarbageOrTruncatedCredentials() {
        assertFalse(PasswordHasher.verify("whatever", "not-a-real-hash"));
        assertFalse(PasswordHasher.verify("whatever", "pbkdf2-sha256$notanumber$aa$bb"));
        assertFalse(PasswordHasher.verify("whatever", "pbkdf2-sha256$210000$zz$bb")); // bad hex
        assertFalse(PasswordHasher.verify("whatever", "pbkdf2-sha256$210000$aa")); // too few parts
    }

    @Test
    public void verify_rejectsAbsurdIterationCountsToAvoidDenialOfService() {
        String bogus = "pbkdf2-sha256$999999999999$" + "aa".repeat(16) + "$" + "bb".repeat(32);
        assertFalse(PasswordHasher.isPbkdf2(bogus));
        assertFalse(PasswordHasher.verify("whatever", bogus));
    }

    @Test
    public void legacySha256Hash_verifiesOnceAndIsFlaggedForUpgrade() {
        // Legacy format written by the pre-Phase-1 code: unsalted hex SHA-256 of the raw password.
        String legacy = sha256Hex("legacy-password");
        assertTrue(PasswordHasher.isLegacyHash(legacy));
        assertTrue(PasswordHasher.hasRecognizedFormat(legacy));
        assertTrue(PasswordHasher.verify("legacy-password", legacy));
        assertFalse(PasswordHasher.verify("wrong-password", legacy));
        assertTrue("a legacy hash must be flagged for upgrade", PasswordHasher.needsUpgrade(legacy));
    }

    @Test
    public void upgradedCredential_stopsAcceptingTheOldFormatSemantics() {
        String legacy = sha256Hex("migrate-me");
        assertTrue(PasswordHasher.verify("migrate-me", legacy));
        String upgraded = PasswordHasher.hash("migrate-me");
        assertNotEquals(legacy, upgraded);
        assertTrue(PasswordHasher.verify("migrate-me", upgraded));
        assertFalse(PasswordHasher.needsUpgrade(upgraded));
    }

    @Test
    public void hash_rejectsEmptyPassword() {
        try {
            PasswordHasher.hash("");
            fail("expected IllegalArgumentException for empty password");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void hash_rejectsNullPassword() {
        try {
            PasswordHasher.hash(null);
            fail("expected IllegalArgumentException for null password");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /** Minimal legacy-format helper mirroring the pre-Phase-1 hashPassword() for test fixtures. */
    private static String sha256Hex(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
