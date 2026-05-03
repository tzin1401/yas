package com.yas.location.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.CountryService;
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
class CountryStorefrontControllerTest {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryStorefrontController countryStorefrontController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(countryStorefrontController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void listCountries_shouldReturnOk() throws Exception {
        when(countryService.findAllCountries()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/storefront/countries"))
            .andExpect(status().isOk());
    }
}
