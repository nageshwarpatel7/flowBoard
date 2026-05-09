package com.flowboard.payment_service.service;

import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.*;
import com.flowboard.payment_service.entity.Plan;
import com.flowboard.payment_service.entity.Subscription;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PlanRepository         planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    // FIX: hardcoded localhost URLs replaced with @Value-injected properties.
    //      Defaults to localhost for local dev; override via env vars in production.
    @Value("${payment.success-url:http://localhost:4200/payment/success?session_id={CHECKOUT_SESSION_ID}}")
    private String successUrl;

    @Value("${payment.cancel-url:http://localhost:4200/payment/cancel}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(this::toPlanResponse).toList();
    }

    @Override
    public SubscriptionResponse getSubscription(Long userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));
        return toSubResponse(sub);
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(
            CreateCheckoutSessionRequest request, Long userId) {

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new CustomException("Plan not found", HttpStatus.NOT_FOUND));

        Subscription existing = subscriptionRepository.findByUserId(userId).orElse(null);
        String customerId = (existing != null) ? existing.getStripeCustomerId() : null;

        String priceId = "YEARLY".equals(request.getBillingCycle())
                ? plan.getStripePriceIdYearly()
                : plan.getStripePriceIdMonthly();

        if (priceId == null || priceId.isBlank()) {
            throw new CustomException(
                    "Stripe price ID not configured for plan " + plan.getName(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    // FIX: uses injected successUrl / cancelUrl instead of hardcoded literals
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("userId",       String.valueOf(userId))
                    .putMetadata("planId",        String.valueOf(plan.getId()))
                    .putMetadata("billingCycle",  request.getBillingCycle());

            if (customerId != null) {
                params.setCustomer(customerId);
            }

            Session session = Session.create(params.build());
            log.info("Checkout session created: {} for userId={}", session.getId(), userId);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe checkout session failed: {}", e.getMessage());
            throw new CustomException(
                    "Payment gateway error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String stripeSignature) {
        Event event;
        try {
            // NOTE: payload must be the RAW bytes-as-string from HttpServletRequest,
            //       not re-encoded via Spring's @RequestBody.  See PaymentController.
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new CustomException("Invalid webhook signature", HttpStatus.BAD_REQUEST);
        }

        log.info("Stripe webhook received: type={}", event.getType());

        switch (event.getType()) {

            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                activateSubscription(session);
            }

            case "invoice.payment_succeeded" -> {
                Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                recordPayment(invoice, "SUCCEEDED");
                renewSubscription(invoice);
            }

            case "invoice.payment_failed" -> {
                Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                recordPayment(invoice, "FAILED");
                markPastDue(invoice.getCustomer());
            }

            case "customer.subscription.deleted" -> {
                com.stripe.model.Subscription stripeSub =
                        (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                                .getObject().orElseThrow();
                cancelByStripeId(stripeSub.getId());
            }

            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelSubscription(Long userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(
                        "No subscription found", HttpStatus.NOT_FOUND));

        if (sub.getStripeSubscriptionId() != null) {
            try {
                com.stripe.model.Subscription stripeSub =
                        com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());
                stripeSub.update(SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true).build());
            } catch (StripeException e) {
                log.error("Failed to cancel Stripe subscription: {}", e.getMessage());
            }
        }

        sub.setStatus("CANCELLED");
        sub.setCancelledAt(java.time.LocalDateTime.now());
        subscriptionRepository.save(sub);
        log.info("Subscription cancelled for userId={}", userId);
    }

    @Override
    public boolean hasFeature(Long userId, String feature) {
        Subscription sub = subscriptionRepository.findByUserId(userId).orElse(null);
        if (sub == null || !"ACTIVE".equals(sub.getStatus())) return false;
        Plan plan = sub.getPlan();
        return switch (feature) {
            case "ADVANCED_ANALYTICS" -> plan.isHasAdvancedAnalytics();
            case "PRIORITY_SUPPORT"   -> plan.isHasPrioritySupport();
            case "CUSTOM_FIELDS"      -> plan.isHasCustomFields();
            case "AUTOMATION"         -> plan.isHasAutomation();
            default -> false;
        };
    }

    @Override
    public List<PaymentRecord> getPaymentHistory(Long userId) {
        return paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void activateSubscription(Session session) {
        Long userId = Long.parseLong(session.getMetadata().get("userId"));
        Long planId = Long.parseLong(session.getMetadata().get("planId"));
        String cycle = session.getMetadata().getOrDefault("billingCycle", "MONTHLY");

        Plan plan = planRepository.findById(planId).orElseThrow();

        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElse(Subscription.builder().userId(userId).build());

        LocalDate now = LocalDate.now();
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        sub.setBillingCycle(cycle);
        sub.setStripeCustomerId(session.getCustomer());
        sub.setStripeSubscriptionId(session.getSubscription());
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(
                "YEARLY".equals(cycle) ? now.plusYears(1) : now.plusMonths(1));

        subscriptionRepository.save(sub);
        log.info("Subscription activated: userId={} plan={}", userId, plan.getName());
    }

    private void renewSubscription(Invoice invoice) {
        subscriptionRepository.findByStripeCustomerId(invoice.getCustomer())
                .ifPresent(sub -> {
                    sub.setStatus("ACTIVE");
                    LocalDate now = LocalDate.now();
                    sub.setCurrentPeriodStart(now);
                    sub.setCurrentPeriodEnd(
                            "YEARLY".equals(sub.getBillingCycle())
                                    ? now.plusYears(1)
                                    : now.plusMonths(1));
                    subscriptionRepository.save(sub);
                });
    }

    private void recordPayment(Invoice invoice, String status) {
        subscriptionRepository.findByStripeCustomerId(invoice.getCustomer())
                .ifPresent(sub -> {
                    PaymentRecord record = PaymentRecord.builder()
                            .userId(sub.getUserId())
                            .stripePaymentIntentId(invoice.getPaymentIntent())
                            .amount(BigDecimal.valueOf(invoice.getAmountPaid())
                                    .divide(BigDecimal.valueOf(100)))
                            .currency(invoice.getCurrency().toUpperCase())
                            .status(status)
                            .description("Invoice " + invoice.getId())
                            .build();
                    paymentRecordRepository.save(record);
                });
    }

    private void markPastDue(String customerId) {
        subscriptionRepository.findByStripeCustomerId(customerId)
                .ifPresent(sub -> {
                    sub.setStatus("PAST_DUE");
                    subscriptionRepository.save(sub);
                });
    }

    private void cancelByStripeId(String stripeSubId) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubId)
                .ifPresent(sub -> {
                    sub.setStatus("CANCELLED");
                    subscriptionRepository.save(sub);
                });
    }

    private Subscription createFreeSubscription(Long userId) {
        Plan freePlan = planRepository.findByName("FREE")
                .orElseThrow(() -> new CustomException(
                        "FREE plan not seeded", HttpStatus.INTERNAL_SERVER_ERROR));
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(freePlan)
                .status("ACTIVE")
                .billingCycle("MONTHLY")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusYears(100))
                .build();
        return subscriptionRepository.save(sub);
    }

    private PlanResponse toPlanResponse(Plan p) {
        return PlanResponse.builder()
                .id(p.getId()).name(p.getName()).displayName(p.getDisplayName())
                .description(p.getDescription()).priceMonthly(p.getPriceMonthly())
                .priceYearly(p.getPriceYearly()).maxWorkspaces(p.getMaxWorkspaces())
                .maxBoardsPerWorkspace(p.getMaxBoardsPerWorkspace())
                .maxMembersPerWorkspace(p.getMaxMembersPerWorkspace())
                .hasAdvancedAnalytics(p.isHasAdvancedAnalytics())
                .hasPrioritySupport(p.isHasPrioritySupport())
                .hasCustomFields(p.isHasCustomFields())
                .hasAutomation(p.isHasAutomation())
                .build();
    }

    private SubscriptionResponse toSubResponse(Subscription s) {
        Plan p = s.getPlan();
        return SubscriptionResponse.builder()
                .id(s.getId()).userId(s.getUserId())
                .planName(p.getName()).planDisplayName(p.getDisplayName())
                .status(s.getStatus()).billingCycle(s.getBillingCycle())
                .currentPeriodEnd(s.getCurrentPeriodEnd())
                .hasAdvancedAnalytics(p.isHasAdvancedAnalytics())
                .hasPrioritySupport(p.isHasPrioritySupport())
                .hasCustomFields(p.isHasCustomFields())
                .hasAutomation(p.isHasAutomation())
                .maxWorkspaces(p.getMaxWorkspaces())
                .maxBoardsPerWorkspace(p.getMaxBoardsPerWorkspace())
                .build();
    }
}
