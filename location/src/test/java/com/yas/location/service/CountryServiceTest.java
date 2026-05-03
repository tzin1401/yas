package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.location.mapper.CountryMapper;
import com.yas.location.model.Country;
import com.yas.location.repository.CountryRepository;
import com.yas.location.viewmodel.country.CountryListGetVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CountryMapper countryMapper;

    @InjectMocks
    private CountryService countryService;

    private Country country1;

    @BeforeEach
    void setUp() {
        country1 = Country.builder()
            .id(1L)
            .code2("TS")
            .name("country-1")
            .build();
    }

    @Test
    void findAllCountries_shouldReturnList() {
        when(countryRepository.findAll(any(Sort.class))).thenReturn(List.of(country1));
        when(countryMapper.toCountryViewModelFromCountry(country1))
            .thenReturn(CountryVm.fromModel(country1));

        List<CountryVm> result = countryService.findAllCountries();

        assertEquals(1, result.size());
        assertEquals("country-1", result.getFirst().name());
    }

    @Test
    void getPageableCountries_shouldReturnPage() {
        Page<Country> page = new PageImpl<>(List.of(country1));
        when(countryRepository.findAll(any(Pageable.class))).thenReturn(page);

        CountryListGetVm result = countryService.getPageableCountries(0, 10);

        assertEquals(1, result.totalElements());
    }

    @Test
    void create_ValidData_ShouldReturnCountry() {
        CountryPostVm postVm =
            new CountryPostVm("ignored-json-id", "T2", "country-2", "USA", true, true, true, true, true);
        Country mapped = Country.builder().name("country-2").code2("T2").build();
        Country saved = Country.builder().id(2L).name("country-2").code2("T2").build();

        when(countryRepository.existsByCode2IgnoreCase("T2")).thenReturn(false);
        when(countryRepository.existsByNameIgnoreCase("country-2")).thenReturn(false);
        when(countryMapper.toCountryFromCountryPostViewModel(postVm)).thenReturn(mapped);
        when(countryRepository.save(mapped)).thenReturn(saved);

        Country result = countryService.create(postVm);

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void create_DuplicateCode2_ShouldThrowException() {
        CountryPostVm postVm =
            new CountryPostVm("x", "TS", "country-1", "USA", true, true, true, true, true);
        when(countryRepository.existsByCode2IgnoreCase("TS")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> countryService.create(postVm));
    }
}
