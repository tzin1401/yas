package com.yas.location.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.service.StateOrProvinceService;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceListGetVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvincePostVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
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

    @Test
    void getById_shouldReturnOk() throws Exception {
        when(stateOrProvinceService.findById(5L)).thenReturn(
            new StateOrProvinceVm(5L, "n", "c", "t", 1L));
        mockMvc.perform(get("/backoffice/state-or-provinces/5"))
            .andExpect(status().isOk());
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        Country country = Country.builder().id(1L).name("C").build();
        StateOrProvince created = StateOrProvince.builder()
            .id(9L)
            .name("n")
            .code("cd")
            .type("tp")
            .country(country)
            .build();
        when(stateOrProvinceService.createStateOrProvince(any(StateOrProvincePostVm.class))).thenReturn(created);

        StateOrProvincePostVm body =
            StateOrProvincePostVm.builder().name("n").code("cd").type("tp").countryId(1L).build();
        mockMvc.perform(post("/backoffice/state-or-provinces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
            .andExpect(status().isCreated());
    }

    @Test
    void update_shouldReturnNoContent() throws Exception {
        StateOrProvincePostVm body =
            StateOrProvincePostVm.builder().name("n").code("c").type("t").countryId(1L).build();
        mockMvc.perform(put("/backoffice/state-or-provinces/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/backoffice/state-or-provinces/3"))
            .andExpect(status().isNoContent());
    }

    @Test
    void stateCountryNames_shouldReturnOk() throws Exception {
        when(stateOrProvinceService.getStateOrProvinceAndCountryNames(anyList())).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/backoffice/state-or-provinces/state-country-names")
                .param("stateOrProvinceIds", "1", "2"))
            .andExpect(status().isOk());
    }
}
