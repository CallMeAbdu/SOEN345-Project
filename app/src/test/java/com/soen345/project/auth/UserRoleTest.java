package com.soen345.project.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserRoleTest {

    @Test
    public void value_returnsExpectedString() {
        assertEquals("customer", UserRole.CUSTOMER.value());
        assertEquals("administrator", UserRole.ADMINISTRATOR.value());
    }

    @Test
    public void fromValue_withCustomerValue_returnsCustomer() {
        assertEquals(UserRole.CUSTOMER, UserRole.fromValue("customer"));
    }

    @Test
    public void fromValue_isCaseInsensitiveAndTrimmed() {
        assertEquals(UserRole.ADMINISTRATOR, UserRole.fromValue("  AdMiNiStRaToR "));
    }

    @Test
    public void fromValue_withUnknownOrNull_returnsNull() {
        assertNull(UserRole.fromValue("manager"));
        assertNull(UserRole.fromValue(null));
    }
}
