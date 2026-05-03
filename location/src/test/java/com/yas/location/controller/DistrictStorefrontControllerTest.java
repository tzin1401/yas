package com.yas.location.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.DistrictService;
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
class DistrictStorefrontControllerTest {

    @Mock
    private DistrictService districtService;

    @InjectMocks
    private DistrictStorefrontController districtStorefrontController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(districtStorefrontController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void getList_storefrontPath_shouldReturnOk() throws Exception {
        when(districtService.getList(3L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/storefront/district/3"))
            .andExpect(status().isOk());
    }

    @Test
    void getList_backofficePath_shouldReturnOk() throws Exception {
        when(districtService.getList(3L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/backoffice/district/3"))
            .andExpect(status().isOk());
    }
}
