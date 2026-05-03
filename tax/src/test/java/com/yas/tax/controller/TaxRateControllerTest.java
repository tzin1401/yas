package com.yas.tax.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    TaxRateController.class,
    ApiExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
class TaxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TaxRateService taxRateService;

    private TaxRateVm taxRateVm;

    @BeforeEach
    void setUp() {
        taxRateVm = new TaxRateVm(1L, 10.0, "70000", 2L, 3L, 4L);
    }

    @Test
    void getPageableTaxRates_shouldReturnTaxRates() throws Exception {
        TaxRateGetDetailVm detailVm = new TaxRateGetDetailVm(1L, 10.0, "70000", "Standard", "HCM", "VN");
        TaxRateListGetVm listGetVm = new TaxRateListGetVm(
            List.of(detailVm), 0, 10, 1, 1, true);

        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(listGetVm);

        mockMvc.perform(get("/backoffice/tax-rates/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].id").value(1))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].rate").value(10.0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTaxRate_shouldReturnTaxRate() throws Exception {
        when(taxRateService.findById(1L)).thenReturn(taxRateVm);

        mockMvc.perform(get("/backoffice/tax-rates/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rate").value(10.0));
    }

    @Test
    void createTaxRate_whenValid_shouldReturnCreated() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 2L, 3L, 4L);
        TaxClass taxClass = TaxClass.builder().id(2L).name("Standard").build();
        TaxRate taxRate = TaxRate.builder().id(1L).rate(10.0).zipCode("70000").taxClass(taxClass).build();

        when(taxRateService.createTaxRate(any(TaxRatePostVm.class))).thenReturn(taxRate);

        mockMvc.perform(post("/backoffice/tax-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rate").value(10.0));
    }

    @Test
    void updateTaxRate_whenValid_shouldReturnNoContent() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 2L, 3L, 4L);
        
        doNothing().when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-rates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());

        verify(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(1L));
    }

    @Test
    void deleteTaxRate_whenValid_shouldReturnNoContent() throws Exception {
        doNothing().when(taxRateService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-rates/1"))
            .andExpect(status().isNoContent());

        verify(taxRateService).delete(1L);
    }

    @Test
    void getTaxPercentByAddress_shouldReturnPercent() throws Exception {
        when(taxRateService.getTaxPercent(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(10.5);

        mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                .param("taxClassId", "1")
                .param("countryId", "2")
                .param("stateOrProvinceId", "3")
                .param("zipCode", "70000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(10.5));
    }

    @Test
    void getBatchTaxPercentsByAddress_shouldReturnBatchRates() throws Exception {
        when(taxRateService.getBulkTaxRate(anyList(), anyLong(), anyLong(), anyString()))
            .thenReturn(List.of(taxRateVm));

        mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                .param("taxClassIds", "1,2")
                .param("countryId", "2")
                .param("stateOrProvinceId", "3")
                .param("zipCode", "70000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].rate").value(10.0));
    }
}
