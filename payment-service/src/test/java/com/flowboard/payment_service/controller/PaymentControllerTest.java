package com.flowboard.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.*;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PaymentController – MockMvc Tests")
class PaymentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  PaymentService paymentService;

    private static final String BASE    = "/api/v1/payments";
    private static final Long   USER_ID = 1L;

    private PlanResponse freePlan() {
        return PlanResponse.builder()
                .id(1L).name("FREE").displayName("Free")
                .description("Get started for free")
                .priceMonthly(BigDecimal.ZERO).priceYearly(BigDecimal.ZERO)
                .maxWorkspaces(3).maxBoardsPerWorkspace(5).maxMembersPerWorkspace(10)
                .hasAdvancedAnalytics(false).hasPrioritySupport(false)
                .hasCustomFields(false).hasAutomation(false).build();
    }

    private PlanResponse proPlan() {
        return PlanResponse.builder()
                .id(2L).name("PRO").displayName("Pro")
                .description("For growing teams")
                .priceMonthly(new BigDecimal("9.99")).priceYearly(new BigDecimal("99.00"))
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).maxMembersPerWorkspace(50)
                .hasAdvancedAnalytics(true).hasPrioritySupport(false)
                .hasCustomFields(true).hasAutomation(false).build();
    }

    private SubscriptionResponse activeSub() {
        return SubscriptionResponse.builder()
                .id(1L).userId(USER_ID).planName("PRO").planDisplayName("Pro")
                .status("ACTIVE").billingCycle("MONTHLY")
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .hasAdvancedAnalytics(true).hasPrioritySupport(false)
                .hasCustomFields(true).hasAutomation(false)
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).build();
    }

    // ── GET /plans ────────────────────────────────────────────────────────────
    @Test @DisplayName("GET /plans → 200 returns all plans")
    void getPlans_200() throws Exception {
        when(paymentService.getAllPlans()).thenReturn(List.of(freePlan(), proPlan()));

        mvc.perform(get(BASE + "/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("FREE"))
                .andExpect(jsonPath("$[1].name").value("PRO"));
    }

    @Test @DisplayName("GET /plans → 200 empty list when no plans seeded")
    void getPlans_empty() throws Exception {
        when(paymentService.getAllPlans()).thenReturn(List.of());
        mvc.perform(get(BASE + "/plans"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /subscription ─────────────────────────────────────────────────────
    @Test @DisplayName("GET /subscription → 200 active subscription")
    void getSubscription_200() throws Exception {
        when(paymentService.getSubscription(USER_ID)).thenReturn(activeSub());

        mvc.perform(get(BASE + "/subscription").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hasAdvancedAnalytics").value(true));
    }

    @Test @DisplayName("GET /subscription → 400 missing X-User-Id")
    void getSubscription_missingHeader_400() throws Exception {
        mvc.perform(get(BASE + "/subscription")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("GET /subscription → 200 free plan for new user")
    void getSubscription_freePlanNewUser_200() throws Exception {
        SubscriptionResponse freeSub = SubscriptionResponse.builder()
                .id(1L).userId(USER_ID).planName("FREE").planDisplayName("Free")
                .status("ACTIVE").billingCycle("MONTHLY")
                .currentPeriodEnd(LocalDate.now().plusYears(100))
                .hasAdvancedAnalytics(false).build();
        when(paymentService.getSubscription(USER_ID)).thenReturn(freeSub);

        mvc.perform(get(BASE + "/subscription").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("FREE"));
    }

    // ── POST /checkout ────────────────────────────────────────────────────────
    @Test @DisplayName("POST /checkout → 201 returns Stripe checkout URL")
    void createCheckout_201() throws Exception {
        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(2L); req.setBillingCycle("MONTHLY");

        CheckoutSessionResponse resp = CheckoutSessionResponse.builder()
                .sessionId("cs_test_abc123")
                .checkoutUrl("https://checkout.stripe.com/pay/cs_test_abc123").build();
        when(paymentService.createCheckoutSession(any(), eq(USER_ID))).thenReturn(resp);

        mvc.perform(post(BASE + "/checkout").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("cs_test_abc123"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_abc123"));
    }

    @Test @DisplayName("POST /checkout → 404 plan not found")
    void createCheckout_planNotFound_404() throws Exception {
        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(999L); req.setBillingCycle("MONTHLY");
        when(paymentService.createCheckoutSession(any(), anyLong()))
                .thenThrow(new CustomException("Plan not found", HttpStatus.NOT_FOUND));

        mvc.perform(post(BASE + "/checkout").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("POST /checkout → 500 Stripe gateway error")
    void createCheckout_stripeError_500() throws Exception {
        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(2L); req.setBillingCycle("YEARLY");
        when(paymentService.createCheckoutSession(any(), anyLong()))
                .thenThrow(new CustomException("Stripe error", HttpStatus.INTERNAL_SERVER_ERROR));

        mvc.perform(post(BASE + "/checkout").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test @DisplayName("POST /checkout → 400 missing X-User-Id")
    void createCheckout_missingHeader_400() throws Exception {
        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(2L); req.setBillingCycle("MONTHLY");

        mvc.perform(post(BASE + "/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /cancel ──────────────────────────────────────────────────────────
    @Test @DisplayName("POST /cancel → 200 subscription cancelled")
    void cancelSubscription_200() throws Exception {
        doNothing().when(paymentService).cancelSubscription(USER_ID);

        mvc.perform(post(BASE + "/cancel").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cancel")));
    }

    @Test @DisplayName("POST /cancel → 404 no active subscription")
    void cancelSubscription_noSub_404() throws Exception {
        doThrow(new CustomException("No subscription found", HttpStatus.NOT_FOUND))
                .when(paymentService).cancelSubscription(USER_ID);

        mvc.perform(post(BASE + "/cancel").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── GET /feature/{feature} ────────────────────────────────────────────────
    @Test @DisplayName("GET /feature/{feature} → 200 true for Pro analytics")
    void hasFeature_true_200() throws Exception {
        when(paymentService.hasFeature(USER_ID, "ADVANCED_ANALYTICS")).thenReturn(true);

        mvc.perform(get(BASE + "/feature/ADVANCED_ANALYTICS").header("X-User-Id", USER_ID))
                .andExpect(status().isOk()).andExpect(content().string("true"));
    }

    @Test @DisplayName("GET /feature/{feature} → 200 false for Free plan user")
    void hasFeature_false_200() throws Exception {
        when(paymentService.hasFeature(USER_ID, "AUTOMATION")).thenReturn(false);

        mvc.perform(get(BASE + "/feature/AUTOMATION").header("X-User-Id", USER_ID))
                .andExpect(status().isOk()).andExpect(content().string("false"));
    }

    // ── GET /history ──────────────────────────────────────────────────────────
    @Test @DisplayName("GET /history → 200 payment records")
    void getPaymentHistory_200() throws Exception {
        PaymentRecord record = PaymentRecord.builder()
                .id(1L).userId(USER_ID).amount(new BigDecimal("9.99"))
                .currency("USD").status("SUCCEEDED")
                .stripePaymentIntentId("pi_abc123")
                .description("Invoice inv_001").createdAt(LocalDateTime.now()).build();
        when(paymentService.getPaymentHistory(USER_ID)).thenReturn(List.of(record));

        mvc.perform(get(BASE + "/history").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test @DisplayName("GET /history → 200 empty history for new user")
    void getPaymentHistory_empty() throws Exception {
        when(paymentService.getPaymentHistory(USER_ID)).thenReturn(List.of());
        mvc.perform(get(BASE + "/history").header("X-User-Id", USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── POST /webhook ─────────────────────────────────────────────────────────
    @Test @DisplayName("POST /webhook → 200 valid Stripe event")
    void webhook_200() throws Exception {
        doNothing().when(paymentService).handleWebhook(anyString(), anyString());

        mvc.perform(post(BASE + "/webhook").with(csrf())
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test @DisplayName("POST /webhook → 400 invalid Stripe signature")
    void webhook_invalidSignature_400() throws Exception {
        doThrow(new CustomException("Invalid webhook signature", HttpStatus.BAD_REQUEST))
                .when(paymentService).handleWebhook(anyString(), anyString());

        mvc.perform(post(BASE + "/webhook").with(csrf())
                        .header("Stripe-Signature", "bad-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /webhook → 200 handles invoice.payment_failed event")
    void webhook_invoicePaymentFailed_200() throws Exception {
        doNothing().when(paymentService).handleWebhook(anyString(), anyString());

        mvc.perform(post(BASE + "/webhook").with(csrf())
                        .header("Stripe-Signature", "t=456,v1=def")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"invoice.payment_failed\"}"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /webhook → 200 handles subscription.deleted event")
    void webhook_subscriptionDeleted_200() throws Exception {
        doNothing().when(paymentService).handleWebhook(anyString(), anyString());

        mvc.perform(post(BASE + "/webhook").with(csrf())
                        .header("Stripe-Signature", "t=789,v1=ghi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"customer.subscription.deleted\"}"))
                .andExpect(status().isOk());
    }
}