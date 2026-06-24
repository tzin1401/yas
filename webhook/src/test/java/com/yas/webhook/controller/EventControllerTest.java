package com.yas.webhook.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.service.EventService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private EventController eventController;

    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventController = new EventController(eventService);
    }

    @Test
    void listEvents_shouldReturnOk() {
        List<EventVm> expected = Collections.emptyList();
        when(eventService.findAllEvents()).thenReturn(expected);
        
        ResponseEntity<List<EventVm>> response = eventController.listWebhooks();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }
}
