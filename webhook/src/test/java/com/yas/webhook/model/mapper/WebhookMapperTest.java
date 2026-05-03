package com.yas.webhook.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class WebhookMapperTest {

    private final WebhookMapper mapper = Mappers.getMapper(WebhookMapper.class);

    @Test
    void toWebhookVm_shouldMapCorrectly() {
        Webhook webhook = new Webhook();
        webhook.setId(1L);
        webhook.setPayloadUrl("http://test.com");

        WebhookVm vm = mapper.toWebhookVm(webhook);

        assertEquals(1L, vm.getId());
        assertEquals("http://test.com", vm.getPayloadUrl());
    }

    @Test
    void toWebhookEventVms_shouldMapCorrectly() {
        WebhookEvent event = new WebhookEvent();
        event.setEventId(1L);

        List<EventVm> vms = mapper.toWebhookEventVms(List.of(event));

        assertEquals(1, vms.size());
        assertEquals(1L, vms.get(0).getId());
    }

    @Test
    void toWebhookEventVms_shouldReturnEmptyList_whenInputIsEmpty() {
        assertTrue(mapper.toWebhookEventVms(Collections.emptyList()).isEmpty());
        assertTrue(mapper.toWebhookEventVms(null).isEmpty());
    }

    @Test
    void toWebhookListGetVm_shouldMapCorrectly() {
        Webhook webhook = new Webhook();
        webhook.setId(1L);
        Page<Webhook> page = new PageImpl<>(List.of(webhook), PageRequest.of(0, 10), 1);

        WebhookListGetVm result = mapper.toWebhookListGetVm(page, 0, 10);

        assertEquals(1, result.getWebhooks().size());
        assertEquals(0, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());
    }
}
