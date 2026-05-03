package com.yas.location.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.location.service.StateOrProvinceService;
import com.yas.location.utils.Constants;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StateOrProvinceStoreFrontController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StateOrProvinceStoreFrontControllerTest {

    @MockitoBean
    private StateOrProvinceService stateOrProvinceService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetStateOrProvince_thenReturnOk() throws Exception {
        given(stateOrProvinceService.getAllByCountryId(1L)).willReturn(List.of());

        this.mockMvc.perform(get(Constants.ApiConstant.STATE_OR_PROVINCES_STOREFRONT_URL + "/1"))
            .andExpect(status().isOk());
    }
}
