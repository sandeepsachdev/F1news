package com.f1news.controller;

import com.f1news.model.dto.PushSubscriptionRequest;
import com.f1news.service.WebPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final WebPushService webPushService;

    public NotificationController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicVapidKey()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody PushSubscriptionRequest request) {
        webPushService.subscribe(request);
        return ResponseEntity.ok(Map.of("status", "subscribed"));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            webPushService.unsubscribe(endpoint);
        }
        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("subscribers", webPushService.getSubscriberCount()));
    }
}
