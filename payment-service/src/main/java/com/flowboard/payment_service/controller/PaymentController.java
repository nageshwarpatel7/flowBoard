package com.flowboard.payment_service.controller;

import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.PaymentRecord;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── Plans ─────────────────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(paymentService.getAllPlans());
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(paymentService.getSubscription(resolve(userId)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(
            @RequestBody CreateCheckoutSessionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createCheckoutSession(request, resolve(userId)));
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelSubscription(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        paymentService.cancelSubscription(resolve(userId));
        return ResponseEntity.ok("Subscription cancelled. Access continues until period end.");
    }

    @GetMapping("/feature/{feature}")
    public ResponseEntity<Boolean> hasFeature(
            @PathVariable String feature,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(paymentService.hasFeature(resolve(userId), feature));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentRecord>> getHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(resolve(userId)));
    }

    // ── Stripe Webhook (public — no JWT) ─────────────────────────────────────

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature) {
        paymentService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok("Webhook received");
    }

    private Long resolve(Long userId) {
        if (userId != null) return userId;
        throw new CustomException("X-User-Id header is required", HttpStatus.BAD_REQUEST);
    }
}