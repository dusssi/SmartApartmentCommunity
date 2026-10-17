package com.apartment.community.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * P0 fix #5: registration and visitor phone numbers must be carried through the data model
 * instead of being silently discarded. These are plain model checks; the end-to-end persistence
 * path (SharedPreferences JSON round trip) is covered by code review of
 * {@code PersistentRepository.saveList()/loadAll()} and by {@link
 * com.apartment.community.security.AccountStoreTest}, which exercises the identical account-phone
 * path used by {@code PersistentRepository}.
 */
public class VisitorAndUserPhoneTest {

    @Test
    public void visitor_storesThePhoneNumberWhenProvided() {
        Visitor v = new Visitor("V-1", "Jordan", "C-204", "Delivery", "2026-10-20 10:00",
                "9876543210", "Expected");
        assertEquals("9876543210", v.phone);
    }

    @Test
    public void visitor_defaultsToEmptyPhoneRatherThanNull() {
        Visitor withNull = new Visitor("V-1", "Jordan", "C-204", "Delivery", "2026-10-20 10:00",
                null, "Expected");
        assertEquals("", withNull.phone);

        // Legacy 6-arg constructor (pre-Phase-1 records without a phone) must still work and
        // default to empty, not null, so UI code can safely call phone.isEmpty().
        Visitor legacyCtor = new Visitor("V-2", "Alex", "C-205", "Guest", "2026-10-21 09:00", "Expected");
        assertEquals("", legacyCtor.phone);
    }

    @Test
    public void user_storesPhoneAndDefaultsToEmptyForTheLegacyConstructor() {
        User withPhone = new User("u1", "Priya", "priya@example.com", "B-101", "555-1234", CommunityRole.RESIDENT);
        assertEquals("555-1234", withPhone.phone);

        User legacyCtor = new User("u2", "Sam", "sam@example.com", "C-3", CommunityRole.RESIDENT);
        assertEquals("", legacyCtor.phone);
    }
}
