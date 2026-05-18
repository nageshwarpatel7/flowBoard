package com.flowboard.payment_service.repository;
import com.flowboard.payment_service.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
    Optional<Subscription> findByStripeSubscriptionId(String subscriptionId);
    Optional<Subscription> findByStripeCustomerId(String customerId);
}