package com.flowboard.payment_service.service;

import com.flowboard.payment_service.dto.CheckoutSessionResponse;
import com.flowboard.payment_service.dto.CreateCheckoutSessionRequest;
import com.flowboard.payment_service.dto.PlanResponse;
import com.flowboard.payment_service.dto.SubscriptionResponse;

import java.util.List;

public interface PaymentService {
    List<PlanResponse> getAllPlans();
    SubscriptionResponse getSubscription(Long userId);
    CheckoutSessionResponse createCheckoutSession(
            CreateCheckoutSessionRequest request, Long userId);
    void handleWebhook(String payload, String stripeSignature);
    void cancelSubscription(Long userId);
    boolean hasFeature(Long userId, String feature);
    List<com.flowboard.payment_service.entity.PaymentRecord> getPaymentHistory(Long userId);
}
