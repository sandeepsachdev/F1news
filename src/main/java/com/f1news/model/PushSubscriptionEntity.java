package com.f1news.model;

import jakarta.persistence.*;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 1024, nullable = false)
    private String endpoint;

    @Column(name = "p256dh", length = 512)
    private String p256dh;

    @Column(name = "auth_key", length = 256)
    private String auth;

    public PushSubscriptionEntity() {}

    public PushSubscriptionEntity(String endpoint, String p256dh, String auth) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public Long getId() { return id; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }

    public void setId(Long id) { this.id = id; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }
    public void setAuth(String auth) { this.auth = auth; }
}
