package com.yas.rating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void testEqualsAndHashCode() {
        Rating r1 = new Rating();
        r1.setId(1L);
        Rating r2 = new Rating();
        r2.setId(1L);
        Rating r3 = new Rating();
        r3.setId(2L);

        assertEquals(r1, r2);
        assertEquals(r1, r1);
        assertNotEquals(r1, r3);
        assertNotEquals(r1, new Object());
        assertNotEquals(r1, null);
        
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
