package com.yas.location.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.StateOrProvinceService;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceListGetVm;
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
class StateOrProvinceControllerTest {

    @Mock
    private StateOrProvinceService stateOrProvinceService;

    @InjectMocks
    private StateOrProvinceController stateOrProvinceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stateOrProvinceController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void testListStateOrProvinces_shouldReturnOk() throws Exception {
        when(stateOrProvinceService.getAllByCountryId(nullable(Long.class))).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/backoffice/state-or-provinces"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetPageableStateOrProvinces_shouldReturnOk() throws Exception {
        StateOrProvinceListGetVm expected =
            new StateOrProvinceListGetVm(Collections.emptyList(), 0, 10, 0, 0, true);
        when(stateOrProvinceService.getPageableStateOrProvinces(anyInt(), anyInt(), nullable(Long.class)))
            .thenReturn(expected);
        mockMvc.perform(get("/backoffice/state-or-provinces/paging"))
            .andExpect(status().isOk());
    }
}
