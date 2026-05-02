package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @Test
    void format_withDefaultPattern_shouldReturnFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 25, 15, 30, 0);
        String result = DateTimeUtils.format(dateTime);
        assertEquals("25-10-2023_15-30-00", result);
    }

    @Test
    void format_withCustomPattern_shouldReturnFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 25, 15, 30, 0);
        String result = DateTimeUtils.format(dateTime, "yyyy/MM/dd HH:mm");
        assertEquals("2023/10/25 15:30", result);
    }
}