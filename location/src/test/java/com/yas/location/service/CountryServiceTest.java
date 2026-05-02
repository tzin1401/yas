package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.model.Country;
import com.yas.location.repository.CountryRepository;
import com.yas.location.viewmodel.country.CountryListGetVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;
    
    @InjectMocks
    private CountryService countryService;

    private Country country1;

    @BeforeEach
    void setUp() {
        country1 = Country.builder().id("C1").code2("TS").name("country-1").build();
    }

    @Test
    void findAllCountries_shouldReturnList() {
        when(countryRepository.findAll()).thenReturn(List.of(country1));
        
        List<CountryVm> result = countryService.findAllCountries();
        
        assertEquals(1, result.size());
        assertEquals("country-1", result.get(0).name());
    }

    @Test
    void getPageableCountries_shouldReturnPage() {
        Page<Country> page = new PageImpl<>(List.of(country1));
        when(countryRepository.findCountriesByName(any(), any(Pageable.class))).thenReturn(page);
        
        CountryListGetVm result = countryService.getPageableCountries(0, 10, "name");
        
        assertEquals(1, result.totalElements());
    }

    @Test
    void create_ValidData_ShouldReturnCountry() {
        CountryPostVm postVm = new CountryPostVm("C2", "country-2", "TS", true);
        when(countryRepository.existsById("C2")).thenReturn(false);
        when(countryRepository.save(any())).thenReturn(country1);
        
        CountryVm result = countryService.create(postVm);
        
        assertNotNull(result);
    }

    @Test
    void create_DuplicateId_ShouldThrowException() {
        CountryPostVm postVm = new CountryPostVm("C1", "country-1", "TS", true);
        when(countryRepository.existsById("C1")).thenReturn(true);
        
        assertThrows(DuplicatedException.class, () -> countryService.create(postVm));
    }
}
