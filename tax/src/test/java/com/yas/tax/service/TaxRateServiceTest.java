package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private LocationService locationService;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxRateService taxRateService;

    @Test
    void createTaxRate_shouldSaveTaxRate_whenTaxClassExists() {
        TaxRatePostVm postVm = new TaxRatePostVm(7.5, "70000", 1L, 2L, 3L);
        TaxClass taxClass = taxClass(1L, "Standard");
        TaxRate saved = taxRate(10L, 7.5, "70000", 2L, 3L, taxClass);
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(saved);

        TaxRate result = taxRateService.createTaxRate(postVm);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createTaxRate_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        TaxRatePostVm postVm = new TaxRatePostVm(7.5, "70000", 99L, 2L, 3L);
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.createTaxRate(postVm))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void updateTaxRate_shouldSaveExistingTaxRate_whenTaxClassExists() {
        TaxClass oldTaxClass = taxClass(1L, "Old");
        TaxClass newTaxClass = taxClass(2L, "New");
        TaxRate taxRate = taxRate(10L, 5.0, "10000", 4L, 5L, oldTaxClass);
        TaxRatePostVm postVm = new TaxRatePostVm(8.25, "70000", 2L, 6L, 7L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(2L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(2L)).thenReturn(newTaxClass);

        taxRateService.updateTaxRate(postVm, 10L);

        assertThat(taxRate.getRate()).isEqualTo(8.25);
        assertThat(taxRate.getZipCode()).isEqualTo("70000");
        assertThat(taxRate.getTaxClass()).isEqualTo(newTaxClass);
        assertThat(taxRate.getStateOrProvinceId()).isEqualTo(6L);
        assertThat(taxRate.getCountryId()).isEqualTo(7L);
        verify(taxRateRepository).save(taxRate);
    }

    @Test
    void updateTaxRate_shouldThrowNotFound_whenTaxRateDoesNotExist() {
        TaxRatePostVm postVm = new TaxRatePostVm(8.25, "70000", 2L, 6L, 7L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 10L))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void updateTaxRate_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        TaxRate taxRate = taxRate(10L, 5.0, "10000", 4L, 5L, taxClass(1L, "Old"));
        TaxRatePostVm postVm = new TaxRatePostVm(8.25, "70000", 99L, 6L, 7L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 10L))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteExistingTaxRate() {
        when(taxRateRepository.existsById(1L)).thenReturn(true);

        taxRateService.delete(1L);

        verify(taxRateRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound_whenTaxRateDoesNotExist() {
        when(taxRateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.delete(99L))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, never()).deleteById(99L);
    }

    @Test
    void findById_shouldReturnTaxRate_whenExists() {
        TaxRate taxRate = taxRate(10L, 7.5, "70000", 2L, 3L, taxClass(1L, "Standard"));
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(10L);

        assertThat(result).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void findById_shouldThrowNotFound_whenMissing() {
        when(taxRateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxRateService.findById(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAll_shouldReturnAllTaxRates() {
        TaxRate taxRate = taxRate(10L, 7.5, "70000", 2L, 3L, taxClass(1L, "Standard"));
        when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).containsExactly(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void getPageableTaxRates_shouldMapLocationDetails_whenLocationDataExists() {
        TaxRate taxRate = taxRate(10L, 7.5, "70000", 2L, 3L, taxClass(1L, "Standard"));
        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxRate), PageRequest.of(0, 10), 1));
        when(locationService.getStateOrProvinceAndCountryNames(List.of(2L)))
            .thenReturn(List.of(new StateOrProvinceAndCountryGetNameVm(2L, "HCM", "Vietnam")));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).containsExactly(
            new TaxRateGetDetailVm(10L, 7.5, "70000", "Standard", "HCM", "Vietnam"));
        assertThat(result.pageNo()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getPageableTaxRates_shouldNotCallLocationService_whenPageIsEmpty() {
        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
        verify(locationService, never()).getStateOrProvinceAndCountryNames(any());
    }

    @Test
    void getTaxPercent_shouldReturnRepositoryValue_whenFound() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "70000", 3L)).thenReturn(7.5);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "70000");

        assertThat(result).isEqualTo(7.5);
    }

    @Test
    void getTaxPercent_shouldReturnZero_whenRepositoryReturnsNull() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "70000", 3L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "70000");

        assertThat(result).isZero();
    }

    @Test
    void getBulkTaxRate_shouldQueryRepositoryWithUniqueTaxClassIds() {
        TaxRate taxRate = taxRate(10L, 7.5, "70000", 2L, 3L, taxClass(1L, "Standard"));
        when(taxRateRepository.getBatchTaxRates(3L, 2L, "70000", Set.of(1L, 2L)))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L, 2L, 1L), 3L, 2L, "70000");

        assertThat(result).containsExactly(TaxRateVm.fromModel(taxRate));
    }

    private static TaxClass taxClass(Long id, String name) {
        return TaxClass.builder()
            .id(id)
            .name(name)
            .build();
    }

    private static TaxRate taxRate(Long id, Double rate, String zipCode, Long stateOrProvinceId, Long countryId,
                                   TaxClass taxClass) {
        return TaxRate.builder()
            .id(id)
            .rate(rate)
            .zipCode(zipCode)
            .stateOrProvinceId(stateOrProvinceId)
            .countryId(countryId)
            .taxClass(taxClass)
            .build();
    }
}
