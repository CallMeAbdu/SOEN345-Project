package com.soen345.project.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PreferredChannelTest {

    @Test
    public void value_returnsExpectedString() {
        assertEquals("EMAIL", PreferredChannel.EMAIL.value());
        assertEquals("SMS", PreferredChannel.SMS.value());
    }

    @Test
    public void fromValue_isCaseInsensitiveAndTrimmed() {
        assertEquals(PreferredChannel.EMAIL, PreferredChannel.fromValue(" email "));
        assertEquals(PreferredChannel.SMS, PreferredChannel.fromValue("Sms"));
    }

    @Test
    public void fromValue_withUnknownOrNull_returnsNull() {
        assertNull(PreferredChannel.fromValue("push"));
        assertNull(PreferredChannel.fromValue(null));
    }
}
