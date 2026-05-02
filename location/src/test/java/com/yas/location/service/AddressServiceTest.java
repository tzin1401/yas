package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.model.Address;
import com.yas.location.model.Country;
import com.yas.location.model.District;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.AddressRepository;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.DistrictRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressGetVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private StateOrProvinceRepository stateOrProvinceRepository;
    
    @InjectMocks
    private AddressService addressService;

    private Address address1;
    private Country country;
    private District district;
    private StateOrProvince stateOrProvince;

    @BeforeEach
    void setUp() {
        country = Country.builder().id("C1").name("country-1").build();
        stateOrProvince = StateOrProvince.builder().id(1L).name("state-1").country(country).build();
        district = District.builder().id(1L).name("district-1").stateOrProvince(stateOrProvince).build();
        address1 = Address.builder().id(1L).city("city-1").district(district).build();
    }

    @Test
    void getAddress_ValidId_ShouldReturnAddress() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address1));
        
        AddressDetailVm result = addressService.getAddress(1L);
        
        assertNotNull(result);
        assertEquals("city-1", result.city());
    }

    @Test
    void getAddress_InvalidId_ShouldThrowNotFoundException() {
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> addressService.getAddress(1L));
    }

    @Test
    void createAddress_ValidData_ShouldReturnAddress() {
        AddressPostVm postVm = new AddressPostVm("name", "phone", "line1", "line2", "city", "zip", 1L, 1L, "C1");
        
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(stateOrProvinceRepository.findById(1L)).thenReturn(Optional.of(stateOrProvince));
        when(countryRepository.findById("C1")).thenReturn(Optional.of(country));
        when(addressRepository.saveAndFlush(any())).thenReturn(address1);

        Address result = addressService.createAddress(postVm);
        
        assertNotNull(result);
    }

    @Test
    void deleteAddress_ValidId_ShouldSuccess() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address1));
        
        addressService.deleteAddress(1L);
        
        verify(addressRepository).delete(address1);
    }
}
