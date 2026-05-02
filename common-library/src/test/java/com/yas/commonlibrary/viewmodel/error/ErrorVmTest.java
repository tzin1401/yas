package com.yas.commonlibrary.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void testConstructor_withThreeArgs() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "Invalid input");
        
        assertEquals("400", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Invalid input", errorVm.detail());
        assertTrue(errorVm.fieldErrors().isEmpty());
    }

    @Test
    void testConstructor_withFourArgs() {
        List<String> errors = List.of("Field cannot be null");
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "Invalid input", errors);
        
        assertEquals("400", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Invalid input", errorVm.detail());
        assertEquals(1, errorVm.fieldErrors().size());
        assertEquals("Field cannot be null", errorVm.fieldErrors().get(0));
    }
}
