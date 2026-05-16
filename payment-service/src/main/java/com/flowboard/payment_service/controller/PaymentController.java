package com.flowboard.payment_service.controller;

import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.PaymentRecord;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
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

    @PostMapping("/checkout/confirm")
    public ResponseEntity<SubscriptionResponse> confirmCheckout(
            @RequestBody ConfirmCheckoutSessionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                paymentService.confirmCheckoutSession(request.getSessionId(), resolve(userId)));
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

    /**
     * FIX: Stripe webhook signature verification (HMAC-SHA256) requires the raw
     * request bytes.  Spring's @RequestBody String decodes through Jackson which
     * can modify whitespace and break the signature.
     *
     * We now read the raw InputStream bytes directly from HttpServletRequest and
     * convert to String without any re-encoding.
     *
     * IMPORTANT: for this to work, Spring must NOT consume the body before we do.
     * Ensure no @RequestBody filter or interceptor reads the stream upstream.
     * If you have a logging filter that reads the body, wrap it with
     * ContentCachingRequestWrapper.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest httpRequest,
            @RequestHeader("Stripe-Signature") String stripeSignature) throws IOException {

        byte[] rawBytes = httpRequest.getInputStream().readAllBytes();
        String payload = new String(rawBytes, StandardCharsets.UTF_8);

        log.debug("Stripe webhook received — payload length={}", rawBytes.length);
        paymentService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok("Webhook received");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolve(Long userId) {
        if (userId != null) return userId;
        throw new CustomException("X-User-Id header is required", HttpStatus.BAD_REQUEST);
    }
}
