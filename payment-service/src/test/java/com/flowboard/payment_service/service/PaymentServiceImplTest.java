package com.flowboard.payment_service.service;

import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.Plan;
import com.flowboard.payment_service.entity.Subscription;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.repository.PaymentRecordRepository;
import com.flowboard.payment_service.repository.PlanRepository;
import com.flowboard.payment_service.repository.SubscriptionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock PlanRepository planRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PaymentRecordRepository paymentRecordRepository;

    @InjectMocks PaymentServiceImpl paymentService;

    private Plan plan;
    private Subscription subscription;

    @BeforeEach
    void setup() {
        plan = Plan.builder()
                .id(1L)
                .name("PRO")
                .displayName("Pro Plan")
                .priceMonthly(BigDecimal.valueOf(10))
                .priceYearly(BigDecimal.valueOf(100))
                .stripePriceIdMonthly("price_month")
                .stripePriceIdYearly("price_year")
                .hasAdvancedAnalytics(true)
                .build();

        subscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .plan(plan)
                .status("ACTIVE")
                .billingCycle("MONTHLY")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .build();
    }

    // ── getAllPlans ─────────────────────────────────────────

    @Test
    void getAllPlans_success() {
        when(planRepository.findAll()).thenReturn(List.of(plan));

        var result = paymentService.getAllPlans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("PRO");
    }

    // ── getSubscription ─────────────────────────────────────

    @Test
    void getSubscription_existing() {
        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.of(subscription));

        var result = paymentService.getSubscription(1L);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getSubscription_createsFree() {
        Plan freePlan = Plan.builder().name("FREE").build();

        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(planRepository.findByName("FREE"))
                .thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        var result = paymentService.getSubscription(1L);

        assertThat(result.getPlanName()).isEqualTo("FREE");
    }

    // ── createCheckoutSession (logic only, no Stripe call) ───

    @Test
    void createCheckoutSession_planNotFound() {
        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(99L);

        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                paymentService.createCheckoutSession(req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCheckoutSession_missingPriceId() {
        plan.setStripePriceIdMonthly(null);

        CreateCheckoutSessionRequest req = new CreateCheckoutSessionRequest();
        req.setPlanId(1L);
        req.setBillingCycle("MONTHLY");

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() ->
                paymentService.createCheckoutSession(req, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Stripe price ID");
    }

    // ── cancelSubscription ───────────────────────────────────

    @Test
    void cancelSubscription_success() {
        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.of(subscription));

        paymentService.cancelSubscription(1L);

        assertThat(subscription.getStatus()).isEqualTo("CANCELLED");
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_notFound() {
        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                paymentService.cancelSubscription(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── hasFeature ───────────────────────────────────────────

    @Test
    void hasFeature_true() {
        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.of(subscription));

        boolean result = paymentService.hasFeature(1L, "ADVANCED_ANALYTICS");

        assertThat(result).isTrue();
    }

    @Test
    void hasFeature_false_whenInactive() {
        subscription.setStatus("CANCELLED");

        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.of(subscription));

        boolean result = paymentService.hasFeature(1L, "ADVANCED_ANALYTICS");

        assertThat(result).isFalse();
    }

    @Test
    void hasFeature_otherFeatures() {
        plan.setHasPrioritySupport(true);
        plan.setHasCustomFields(true);
        plan.setHasAutomation(true);

        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.of(subscription));

        assertThat(paymentService.hasFeature(1L, "PRIORITY_SUPPORT")).isTrue();
        assertThat(paymentService.hasFeature(1L, "CUSTOM_FIELDS")).isTrue();
        assertThat(paymentService.hasFeature(1L, "AUTOMATION")).isTrue();
        assertThat(paymentService.hasFeature(1L, "UNKNOWN_FEATURE")).isFalse();
    }

    @Test
    void hasFeature_noSubscription() {
        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        boolean result = paymentService.hasFeature(1L, "ADVANCED_ANALYTICS");

        assertThat(result).isFalse();
    }

    // ── getPaymentHistory ────────────────────────────────────

    @Test
    void getPaymentHistory_success() {
        when(paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        var result = paymentService.getPaymentHistory(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllPlans returns all plans from repository")
    void getAllPlans_ReturnsList() {
        Plan p = Plan.builder().id(1L).name("FREE").build();
        when(planRepository.findAll()).thenReturn(List.of(p));

        List<PlanResponse> result = paymentService.getAllPlans();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("FREE");
    }

    @Test
    @DisplayName("getSubscription returns existing subscription")
    void getSubscription_Existing_ReturnsSub() {
        Plan p = Plan.builder().name("PRO").build();
        Subscription s = Subscription.builder().userId(1L).plan(p).status("ACTIVE").build();
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(s));

        SubscriptionResponse result = paymentService.getSubscription(1L);
        assertThat(result.getPlanName()).isEqualTo("PRO");
    }

    @Test
    @DisplayName("getSubscription creates free subscription if none exists")
    void getSubscription_NotExists_CreatesFree() {
        Plan freePlan = Plan.builder().name("FREE").displayName("Free Plan").build();
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(planRepository.findByName("FREE")).thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse result = paymentService.getSubscription(1L);
        assertThat(result.getPlanName()).isEqualTo("FREE");
        verify(subscriptionRepository).save(any());
    }
}