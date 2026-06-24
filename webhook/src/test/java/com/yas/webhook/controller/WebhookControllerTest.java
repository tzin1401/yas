package com.yas.webhook.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.service.WebhookService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private WebhookController webhookController;

    @Mock
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookController = new WebhookController(webhookService);
    }

    @Test
    void getPageableWebhooks_shouldReturnOk() {
        WebhookListGetVm expect = WebhookListGetVm.builder().build();
        when(webhookService.getPageableWebhooks(anyInt(), anyInt())).thenReturn(expect);
        
        ResponseEntity<WebhookListGetVm> response = webhookController.getPageableWebhooks(0, 10);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expect, response.getBody());
    }

    @Test
    void listWebhooks_shouldReturnOk() {
        when(webhookService.findAllWebhooks()).thenReturn(Collections.emptyList());
        
        ResponseEntity<?> response = webhookController.listWebhooks();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
