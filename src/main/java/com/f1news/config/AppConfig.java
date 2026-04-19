package com.f1news.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Security;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${vapid.public.key:}")
    private String vapidPublicKey;

    @Value("${vapid.private.key:}")
    private String vapidPrivateKey;

    @Value("${vapid.subject:mailto:admin@f1news.app}")
    private String vapidSubject;

    @Value("${f1.api.base-url:https://api.jolpi.ca/ergast/f1}")
    private String f1ApiBaseUrl;

    @Bean
    public PushService pushService() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            log.warn("VAPID keys not configured — generating ephemeral keys. Set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY env vars for persistence.");
            java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(new org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec("prime256v1"));
            java.security.KeyPair kp = kpg.generateKeyPair();
            // Encode public key as uncompressed point (65 bytes: 0x04 + x + y)
            byte[] pubBytes = nl.martijndwars.webpush.Utils.encode(
                    (org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic());
            byte[] privBytes = nl.martijndwars.webpush.Utils.encode(
                    (org.bouncycastle.jce.interfaces.ECPrivateKey) kp.getPrivate());
            String pub = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(pubBytes);
            String priv = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(privBytes);
            log.info("Generated ephemeral VAPID public key: {}", pub);
            return new PushService(pub, priv, vapidSubject);
        }

        return new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }

    @Bean
    public WebClient f1WebClient() {
        return WebClient.builder()
                .baseUrl(f1ApiBaseUrl)
                .defaultHeader("Accept", "application/json")
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
