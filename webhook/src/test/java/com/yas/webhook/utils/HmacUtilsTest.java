package com.yas.webhook.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class HmacUtilsTest {

    @Test
    void hash_shouldReturnNotNull() throws Exception {
        String hash = HmacUtils.hash("data", "key");
        assertNotNull(hash);
    }
}
