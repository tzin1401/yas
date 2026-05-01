package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Event;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.Collections;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    WebhookRepository webhookRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    WebhookEventRepository webhookEventRepository;
    @Mock
    WebhookEventNotificationRepository webhookEventNotificationRepository;
    @Mock
    WebhookMapper webhookMapper;
    @Mock
    WebhookApi webHookApi;

    @InjectMocks
    WebhookService webhookService;

    private Webhook webhook;
    private WebhookDetailVm webhookDetailVm;

    @BeforeEach
    void setUp() {
        webhook = new Webhook();
        webhook.setId(1L);

        webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(1L);
    }

    @Test
    void getPageableWebhooks_shouldReturnList() {
        Page<Webhook> page = new PageImpl<>(List.of(webhook));
        when(webhookRepository.findAll(any(PageRequest.class))).thenReturn(page);
        WebhookListGetVm expect = WebhookListGetVm.builder().totalElements(1).build();
        when(webhookMapper.toWebhookListGetVm(any(), anyInt(), anyInt())).thenReturn(expect);

        WebhookListGetVm result = webhookService.getPageableWebhooks(0, 10);

        assertEquals(expect, result);
    }

    @Test
    void findAllWebhooks_shouldReturnList() {
        when(webhookRepository.findAll(any(Sort.class))).thenReturn(List.of(webhook));
        WebhookVm vm = new WebhookVm();
        when(webhookMapper.toWebhookVm(any())).thenReturn(vm);

        List<WebhookVm> result = webhookService.findAllWebhooks();

        assertEquals(1, result.size());
    }

    @Test
    void findById_shouldReturnWebhook() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.findById(1L);

        assertEquals(webhookDetailVm, result);
    }

    @Test
    void findById_shouldThrowNotFoundException() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.findById(1L));
    }

    @Test
    void create_shouldReturnWebhookDetailVm() {
        EventVm eventVm = new EventVm();
        eventVm.setId(1L);
        WebhookPostVm postVm = new WebhookPostVm();
        postVm.setEvents(List.of(eventVm));
        
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(webhook);
        when(webhookRepository.save(any())).thenReturn(webhook);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(new Event()));
        when(webhookEventRepository.saveAll(any())).thenReturn(List.of(new WebhookEvent()));
        when(webhookMapper.toWebhookDetailVm(any())).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.create(postVm);

        assertEquals(webhookDetailVm, result);
    }

    @Test
    void create_shouldThrowNotFoundException_whenEventNotFound() {
        EventVm eventVm = new EventVm();
        eventVm.setId(1L);
        WebhookPostVm postVm = new WebhookPostVm();
        postVm.setEvents(List.of(eventVm));
        
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(webhook);
        when(webhookRepository.save(any())).thenReturn(webhook);
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.create(postVm));
    }

    @Test
    void update_shouldUpdateSuccessfully() {
        EventVm eventVm = new EventVm();
        eventVm.setId(1L);
        WebhookPostVm postVm = new WebhookPostVm();
        postVm.setEvents(List.of(eventVm));
        
        webhook.setWebhookEvents(Collections.emptyList());
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toUpdatedWebhook(webhook, postVm)).thenReturn(webhook);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(new Event()));

        webhookService.update(postVm, 1L);

        verify(webhookRepository).save(any());
        verify(webhookEventRepository).deleteAll(any());
        verify(webhookEventRepository).saveAll(any());
    }

    @Test
    void update_shouldThrowNotFoundException() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.update(new WebhookPostVm(), 1L));
    }

    @Test
    void delete_shouldDeleteSuccessfully() {
        when(webhookRepository.existsById(1L)).thenReturn(true);

        webhookService.delete(1L);

        verify(webhookEventRepository).deleteByWebhookId(1L);
        verify(webhookRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException() {
        when(webhookRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> webhookService.delete(1L));
    }

    @Test
    void test_notifyToWebhook_ShouldNotException() {
        WebhookEventNotificationDto notificationDto = WebhookEventNotificationDto
            .builder()
            .notificationId(1L)
            .url("")
            .secret("")
            .build();

        WebhookEventNotification notification = new WebhookEventNotification();
        when(webhookEventNotificationRepository.findById(notificationDto.getNotificationId()))
            .thenReturn(Optional.of(notification));

        webhookService.notifyToWebhook(notificationDto);

        verify(webhookEventNotificationRepository).save(notification);
        verify(webHookApi).notify(notificationDto.getUrl(), notificationDto.getSecret(), notificationDto.getPayload());
    }
}
