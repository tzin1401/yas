package com.yas.location.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.model.Country;
import com.yas.location.service.CountryService;
import com.yas.location.viewmodel.country.CountryListGetVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
        CountryListGetVm expected =
            new CountryListGetVm(Collections.emptyList(), 0, 10, 0, 0, true);
        when(countryService.getPageableCountries(anyInt(), anyInt())).thenReturn(expected);
        mockMvc.perform(get("/backoffice/countries/paging"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetCountry_shouldReturnOk() throws Exception {
        CountryVm vm = new CountryVm(1L, "US", "USA", "USA", true, true, true, true, true);
        when(countryService.findById(1L)).thenReturn(vm);
        mockMvc.perform(get("/backoffice/countries/1"))
            .andExpect(status().isOk());
    }

    @Test
    void testCreateCountry_shouldReturnCreated() throws Exception {
        CountryPostVm body = new CountryPostVm("id", "US", "USA", "USA", true, true, true, true, true);
        Country saved = Country.builder().id(10L).code2("US").name("USA").build();
        when(countryService.create(any(CountryPostVm.class))).thenReturn(saved);

        mockMvc.perform(post("/backoffice/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
            .andExpect(status().isCreated());
    }

    @Test
    void testUpdateCountry_shouldReturnNoContent() throws Exception {
        CountryPostVm body = new CountryPostVm("x", "US", "USA", "USA", true, true, true, true, true);
        mockMvc.perform(put("/backoffice/countries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
            .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCountry_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/backoffice/countries/1"))
            .andExpect(status().isNoContent());
    }
}
