package com.yas.location.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.AddressService;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressGetVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
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
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private MockMvc mockMvc;
    private ObjectWriter objectWriter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    @Test
    void testCreateAddress_whenRequestIsValid_thenReturnOk() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        when(addressService.createAddress(any(AddressPostVm.class))).thenReturn(
            AddressGetVm.builder()
                .id(1L)
                .contactName("contactName")
                .phone("12345678")
                .addressLine1("addressLine1")
                .addressLine2("addressLine2")
                .city("city")
                .zipCode("zipCode")
                .districtId(1L)
                .stateOrProvinceId(1L)
                .countryId(1L)
                .build());

        mockMvc.perform(post("/storefront/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(addressPostVm)))
            .andExpect(status().isOk());
    }

    @Test
    void testUpdateAddress_whenRequestIsValid_thenReturnNoContent() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        mockMvc.perform(put("/storefront/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(addressPostVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void testGetAddressById_shouldReturnOk() throws Exception {
        AddressDetailVm detail = AddressDetailVm.builder()
            .id(1L)
            .city("city")
            .contactName("n")
            .phone("p")
            .addressLine1("a1")
            .addressLine2("a2")
            .zipCode("z")
            .districtId(1L)
            .districtName("d")
            .stateOrProvinceId(1L)
            .stateOrProvinceName("s")
            .countryId(1L)
            .countryName("c")
            .build();
        when(addressService.getAddress(1L)).thenReturn(detail);

        mockMvc.perform(get("/storefront/addresses/1"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetAddressList_shouldReturnOk() throws Exception {
        when(addressService.getAddressList(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/storefront/addresses").param("ids", "1", "2"))
            .andExpect(status().isOk());
    }

    @Test
    void testDeleteAddress_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/storefront/addresses/1"))
            .andExpect(status().isOk());
    }
}
