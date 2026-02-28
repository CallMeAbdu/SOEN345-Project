package com.soen345.project.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserRoleTest {

    @Test
    public void value_returnsExpectedString() {
        assertEquals("CUSTOMER", UserRole.CUSTOMER.value());
        assertEquals("ADMIN", UserRole.ADMIN.value());
    }

    @Test
    public void fromValue_withCustomerValue_returnsCustomer() {
        assertEquals(UserRole.CUSTOMER, UserRole.fromValue("customer"));
    }

    @Test
    public void fromValue_isCaseInsensitiveAndTrimmed() {
        assertEquals(UserRole.ADMIN, UserRole.fromValue("  AdMiNiStRaToR "));
        assertEquals(UserRole.ADMIN, UserRole.fromValue("admin"));
    }

    @Test
    public void fromValue_withUnknownOrNull_returnsNull() {
        assertNull(UserRole.fromValue("manager"));
        assertNull(UserRole.fromValue(null));
    }
}
