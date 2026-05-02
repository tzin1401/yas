package com.yas.location.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.CountryService;
import com.yas.location.viewmodel.country.CountryListGetVm;
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
class CountryControllerTest {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryController countryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(countryController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void testListCountries_shouldReturnOk() throws Exception {
        when(countryService.findAllCountries()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/backoffice/countries"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPageableCountries_shouldReturnOk() throws Exception {
        CountryListGetVm expected = new CountryListGetVm(Collections.emptyList(), 0, 10, 0, 0, true);
        when(countryService.getPageableCountries(anyInt(), anyInt(), anyString())).thenReturn(expected);
        mockMvc.perform(get("/backoffice/countries/paging"))
                .andExpect(status().isOk());
    }
}
