package com.soen345.project.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthSessionTest {

    @Test
    public void constructor_setsEmailAndRole() {
        AuthSession session = new AuthSession("user@example.com", UserRole.CUSTOMER);

        assertEquals("user@example.com", session.getEmail());
        assertEquals(UserRole.CUSTOMER, session.getRole());
    }
}
