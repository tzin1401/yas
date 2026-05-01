package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    @Test
    void findAllTaxClasses_shouldReturnSortedTaxClasses() {
        TaxClass standard = taxClass(1L, "Standard");
        TaxClass reduced = taxClass(2L, "Reduced");
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
            .thenReturn(List.of(reduced, standard));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).containsExactly(TaxClassVm.fromModel(reduced), TaxClassVm.fromModel(standard));
    }

    @Test
    void findById_shouldReturnTaxClass_whenExists() {
        TaxClass taxClass = taxClass(1L, "Standard");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertThat(result).isEqualTo(TaxClassVm.fromModel(taxClass));
    }

    @Test
    void findById_shouldThrowNotFound_whenMissing() {
        when(taxClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxClassService.findById(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldSaveTaxClass_whenNameIsUnique() {
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "Standard");
        TaxClass saved = taxClass(1L, "Standard");
        when(taxClassRepository.existsByName("Standard")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(saved);

        TaxClass result = taxClassService.create(postVm);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void create_shouldThrowDuplicated_whenNameAlreadyExists() {
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "Standard");
        when(taxClassRepository.existsByName("Standard")).thenReturn(true);

        assertThatThrownBy(() -> taxClassService.create(postVm))
            .isInstanceOf(DuplicatedException.class);
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void update_shouldSaveExistingTaxClass_whenNameIsUniqueForOtherRows() {
        TaxClass taxClass = taxClass(1L, "Old");
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "New");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(false);

        taxClassService.update(postVm, 1L);

        assertThat(taxClass.getName()).isEqualTo("New");
        verify(taxClassRepository).save(taxClass);
    }

    @Test
    void update_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "New");
        when(taxClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxClassService.update(postVm, 99L))
            .isInstanceOf(NotFoundException.class);
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowDuplicated_whenNameBelongsToAnotherTaxClass() {
        TaxClass taxClass = taxClass(1L, "Old");
        TaxClassPostVm postVm = new TaxClassPostVm("ignored", "New");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(true);

        assertThatThrownBy(() -> taxClassService.update(postVm, 1L))
            .isInstanceOf(DuplicatedException.class);
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteExistingTaxClass() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taxClassService.delete(99L))
            .isInstanceOf(NotFoundException.class);
        verify(taxClassRepository, never()).deleteById(99L);
    }

    @Test
    void getPageableTaxClasses_shouldReturnPageMetadata() {
        TaxClass taxClass = taxClass(1L, "Standard");
        when(taxClassRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxClass), PageRequest.of(0, 10), 1));

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.taxClassContent()).containsExactly(TaxClassVm.fromModel(taxClass));
        assertThat(result.pageNo()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    private static TaxClass taxClass(Long id, String name) {
        return TaxClass.builder()
            .id(id)
            .name(name)
            .build();
    }
}
