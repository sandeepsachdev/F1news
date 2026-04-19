package com.f1news.service;

import com.f1news.model.PushSubscriptionEntity;
import com.f1news.model.dto.PushSubscriptionRequest;
import com.f1news.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushService pushService;
    private final PushSubscriptionRepository repository;

    @Value("${vapid.public.key:}")
    private String vapidPublicKey;

    public WebPushService(PushService pushService, PushSubscriptionRepository repository) {
        this.pushService = pushService;
        this.repository = repository;
    }

    public String getPublicVapidKey() {
        return vapidPublicKey;
    }

    @Transactional
    public void subscribe(PushSubscriptionRequest request) {
        repository.findByEndpoint(request.endpoint()).ifPresentOrElse(
                existing -> log.debug("Subscription already exists for endpoint"),
                () -> {
                    String p256dh = request.keys() != null ? request.keys().p256dh() : null;
                    String auth = request.keys() != null ? request.keys().auth() : null;
                    repository.save(new PushSubscriptionEntity(request.endpoint(), p256dh, auth));
                    log.info("New push subscription saved");
                }
        );
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        repository.deleteByEndpoint(endpoint);
        log.info("Push subscription removed");
    }

    public void sendNotificationToAll(String title, String body, String url) {
        List<PushSubscriptionEntity> subscriptions = repository.findAll();
        log.info("Sending push notification to {} subscribers", subscriptions.size());

        String payload = buildPayload(title, body, url);

        for (PushSubscriptionEntity sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes()
                );
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("Failed to send notification to {}: {}", sub.getEndpoint(), e.getMessage());
                // Remove invalid/expired subscriptions
                if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("404"))) {
                    repository.deleteByEndpoint(sub.getEndpoint());
                    log.info("Removed expired subscription");
                }
            }
        }
    }

    private String buildPayload(String title, String body, String url) {
        return """
                {"title":"%s","body":"%s","url":"%s","icon":"/images/f1-icon.png"}
                """.formatted(
                title.replace("\"", "'"),
                body.replace("\"", "'"),
                url
        ).trim();
    }

    public long getSubscriberCount() {
        return repository.count();
    }
}
