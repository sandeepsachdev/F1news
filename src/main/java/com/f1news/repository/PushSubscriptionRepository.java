package com.f1news.repository;

import com.f1news.model.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
