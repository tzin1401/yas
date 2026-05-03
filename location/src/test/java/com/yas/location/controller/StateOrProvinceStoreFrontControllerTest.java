package com.yas.location.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.StateOrProvinceService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StateOrProvinceStoreFrontControllerTest {

    @Mock
    private StateOrProvinceService stateOrProvinceService;

    @InjectMocks
    private StateOrProvinceStoreFrontController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void getByCountryId_shouldReturnOk() throws Exception {
        when(stateOrProvinceService.getAllByCountryId(5L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/storefront/state-or-provinces/5"))
            .andExpect(status().isOk());
    }
}
