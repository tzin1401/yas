package com.yas.tax.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
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
class TaxClassControllerTest {

    @Mock
    private TaxClassService taxClassService;

    @InjectMocks
    private TaxClassController taxClassController;

    @Test
    void getPageableTaxClasses_shouldReturnPage() {
        TaxClassListGetVm page = new TaxClassListGetVm(List.of(taxClassVm()), 0, 10, 1, 1, true);
        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(page);

        ResponseEntity<TaxClassListGetVm> response = taxClassController.getPageableTaxClasses(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void listTaxClasses_shouldReturnAllTaxClasses() {
        List<TaxClassVm> taxClasses = List.of(taxClassVm());
        when(taxClassService.findAllTaxClasses()).thenReturn(taxClasses);

        ResponseEntity<List<TaxClassVm>> response = taxClassController.listTaxClasses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taxClasses);
    }

    @Test
    void getTaxClass_shouldReturnTaxClass() {
        TaxClassVm taxClassVm = taxClassVm();
        when(taxClassService.findById(1L)).thenReturn(taxClassVm);

        ResponseEntity<TaxClassVm> response = taxClassController.getTaxClass(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taxClassVm);
    }

    @Test
    void createTaxClass_shouldReturnCreatedTaxClass() {
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "Standard");
        TaxClass taxClass = taxClass();
        when(taxClassService.create(postVm)).thenReturn(taxClass);

        ResponseEntity<TaxClassVm> response = taxClassController.createTaxClass(
            postVm, UriComponentsBuilder.fromUri(URI.create("http://localhost")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasPath("/tax-classes/1");
        assertThat(response.getBody()).isEqualTo(TaxClassVm.fromModel(taxClass));
    }

    @Test
    void updateTaxClass_shouldReturnNoContent() {
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "Standard");

        ResponseEntity<Void> response = taxClassController.updateTaxClass(1L, postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxClassService).update(postVm, 1L);
    }

    @Test
    void deleteTaxClass_shouldReturnNoContent() {
        ResponseEntity<Void> response = taxClassController.deleteTaxClass(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxClassService).delete(1L);
    }

    private static TaxClassVm taxClassVm() {
        return TaxClassVm.fromModel(taxClass());
    }

    private static TaxClass taxClass() {
        return TaxClass.builder()
            .id(1L)
            .name("Standard")
            .build();
    }
}
