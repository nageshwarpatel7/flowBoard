package com.flowboard.card_service.client;

import com.flowboard.card_service.client.dto.NotifyDueDateRequest;
import com.flowboard.card_service.client.dto.NotifyOverdueRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "NOTIFICATION-SERVICE",
            path = "/api/v1/notifications")
public interface NotificationClient {

    @PostMapping("/notify/due-date-body")
    void notifyDueDate(@RequestBody NotifyDueDateRequest request);

    @PostMapping("/notify/overdue-body")
    void notifyOverdue(@RequestBody NotifyOverdueRequest request);
}
