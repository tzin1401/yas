package com.yas.tax.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ContextConfiguration(classes = {
    TaxClassController.class,
    ApiExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
class TaxClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TaxClassService taxClassService;

    private TaxClassVm taxClassVm;

    @BeforeEach
    void setUp() {
        taxClassVm = new TaxClassVm(1L, "Standard");
    }

    @Test
    void getPageableTaxClasses_shouldReturnTaxClasses() throws Exception {
        TaxClassListGetVm listGetVm = new TaxClassListGetVm(
            List.of(taxClassVm), 0, 10, 1, 1, true);

        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(listGetVm);

        mockMvc.perform(get("/backoffice/tax-classes/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxClassContent[0].id").value(1))
            .andExpect(jsonPath("$.taxClassContent[0].name").value("Standard"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listTaxClasses_shouldReturnAllTaxClasses() throws Exception {
        when(taxClassService.findAllTaxClasses()).thenReturn(List.of(taxClassVm));

        mockMvc.perform(get("/backoffice/tax-classes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Standard"));
    }

    @Test
    void getTaxClass_shouldReturnTaxClass() throws Exception {
        when(taxClassService.findById(1L)).thenReturn(taxClassVm);

        mockMvc.perform(get("/backoffice/tax-classes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Standard"));
    }

    @Test
    void createTaxClass_whenValid_shouldReturnCreated() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Standard");
        TaxClass taxClass = TaxClass.builder().id(1L).name("Standard").build();

        when(taxClassService.create(any(TaxClassPostVm.class))).thenReturn(taxClass);

        mockMvc.perform(post("/backoffice/tax-classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Standard"));
    }

    @Test
    void updateTaxClass_whenValid_shouldReturnNoContent() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Standard Updated");
        
        doNothing().when(taxClassService).update(any(TaxClassPostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-classes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());

        verify(taxClassService).update(any(TaxClassPostVm.class), eq(1L));
    }

    @Test
    void deleteTaxClass_whenValid_shouldReturnNoContent() throws Exception {
        doNothing().when(taxClassService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-classes/1"))
            .andExpect(status().isNoContent());

        verify(taxClassService).delete(1L);
    }
}
