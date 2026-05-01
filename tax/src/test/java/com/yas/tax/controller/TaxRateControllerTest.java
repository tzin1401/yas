package com.yas.tax.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class TaxRateControllerTest {

    @Mock
    private TaxRateService taxRateService;

    @InjectMocks
    private TaxRateController taxRateController;

    @Test
    void getPageableTaxRates_shouldReturnPage() {
        TaxRateListGetVm page = new TaxRateListGetVm(
            List.of(new TaxRateGetDetailVm(1L, 7.5, "70000", "Standard", "HCM", "Vietnam")),
            0,
            10,
            1,
            1,
            true);
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(page);

        ResponseEntity<TaxRateListGetVm> response = taxRateController.getPageableTaxRates(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void getTaxRate_shouldReturnTaxRate() {
        TaxRateVm taxRateVm = taxRateVm();
        when(taxRateService.findById(1L)).thenReturn(taxRateVm);

        ResponseEntity<TaxRateVm> response = taxRateController.getTaxRate(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taxRateVm);
    }

    @Test
    void createTaxRate_shouldReturnCreatedTaxRate() {
        TaxRatePostVm postVm = new TaxRatePostVm(7.5, "70000", 1L, 2L, 3L);
        TaxRate taxRate = taxRate();
        when(taxRateService.createTaxRate(postVm)).thenReturn(taxRate);

        ResponseEntity<TaxRateVm> response = taxRateController.createTaxRate(
            postVm, UriComponentsBuilder.fromUri(URI.create("http://localhost")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasPath("/tax-rates/1");
        assertThat(response.getBody()).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void updateTaxRate_shouldReturnNoContent() {
        TaxRatePostVm postVm = new TaxRatePostVm(7.5, "70000", 1L, 2L, 3L);

        ResponseEntity<Void> response = taxRateController.updateTaxRate(1L, postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxRateService).updateTaxRate(postVm, 1L);
    }

    @Test
    void deleteTaxRate_shouldReturnNoContent() {
        ResponseEntity<Void> response = taxRateController.deleteTaxRate(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxRateService).delete(1L);
    }

    @Test
    void getTaxPercentByAddress_shouldReturnTaxPercent() {
        when(taxRateService.getTaxPercent(1L, 2L, 3L, "70000")).thenReturn(7.5);

        ResponseEntity<Double> response = taxRateController.getTaxPercentByAddress(1L, 2L, 3L, "70000");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(7.5);
    }

    @Test
    void getBatchTaxPercentsByAddress_shouldReturnBulkTaxRates() {
        List<TaxRateVm> taxRates = List.of(taxRateVm());
        when(taxRateService.getBulkTaxRate(List.of(1L, 2L), 3L, 4L, "70000")).thenReturn(taxRates);

        ResponseEntity<List<TaxRateVm>> response =
            taxRateController.getBatchTaxPercentsByAddress(List.of(1L, 2L), 3L, 4L, "70000");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taxRates);
    }

    private static TaxRateVm taxRateVm() {
        return TaxRateVm.fromModel(taxRate());
    }

    private static TaxRate taxRate() {
        return TaxRate.builder()
            .id(1L)
            .rate(7.5)
            .zipCode("70000")
            .stateOrProvinceId(2L)
            .countryId(3L)
            .taxClass(TaxClass.builder().id(1L).name("Standard").build())
            .build();
    }
}
