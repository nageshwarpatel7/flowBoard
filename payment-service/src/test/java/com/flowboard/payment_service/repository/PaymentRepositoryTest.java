package com.flowboard.payment_service.repository;

import com.flowboard.payment_service.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Payment Repositories – @DataJpaTest")
class PaymentRepositoryTest {

    @Autowired PlanRepository         planRepo;
    @Autowired SubscriptionRepository subRepo;
    @Autowired PaymentRecordRepository paymentRepo;

    private Plan freePlan, proPlan, businessPlan;
    private Subscription activeSub, cancelledSub;

    @BeforeEach
    void setUp() {
        freePlan = planRepo.save(Plan.builder()
                .name("FREE").displayName("Free").description("Free tier")
                .priceMonthly(BigDecimal.ZERO).priceYearly(BigDecimal.ZERO)
                .maxWorkspaces(3).maxBoardsPerWorkspace(5).maxMembersPerWorkspace(10)
                .hasAdvancedAnalytics(false).hasPrioritySupport(false)
                .hasCustomFields(false).hasAutomation(false).build());

        proPlan = planRepo.save(Plan.builder()
                .name("PRO").displayName("Pro").description("Pro tier")
                .priceMonthly(new BigDecimal("9.99")).priceYearly(new BigDecimal("99.00"))
                .stripePriceIdMonthly("price_pro_monthly").stripePriceIdYearly("price_pro_yearly")
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).maxMembersPerWorkspace(50)
                .hasAdvancedAnalytics(true).hasPrioritySupport(false)
                .hasCustomFields(true).hasAutomation(false).build());

        businessPlan = planRepo.save(Plan.builder()
                .name("BUSINESS").displayName("Business").description("Business tier")
                .priceMonthly(new BigDecimal("29.99")).priceYearly(new BigDecimal("299.00"))
                .stripePriceIdMonthly("price_biz_monthly").stripePriceIdYearly("price_biz_yearly")
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).maxMembersPerWorkspace(-1)
                .hasAdvancedAnalytics(true).hasPrioritySupport(true)
                .hasCustomFields(true).hasAutomation(true).build());

        activeSub = subRepo.save(Subscription.builder()
                .userId(1L).plan(proPlan).status("ACTIVE").billingCycle("MONTHLY")
                .stripeCustomerId("cus_abc123").stripeSubscriptionId("sub_abc123")
                .currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1))
                .createdAt(LocalDateTime.now()).build());

        cancelledSub = subRepo.save(Subscription.builder()
                .userId(2L).plan(freePlan).status("CANCELLED").billingCycle("MONTHLY")
                .stripeCustomerId("cus_def456").stripeSubscriptionId("sub_def456")
                .currentPeriodStart(LocalDate.now().minusMonths(2))
                .currentPeriodEnd(LocalDate.now().minusMonths(1))
                .createdAt(LocalDateTime.now().minusMonths(2))
                .cancelledAt(LocalDateTime.now().minusMonths(1)).build());
    }

    // ── PlanRepository ──────────────────────────────────────────────────────
    @Test @DisplayName("findByName – finds FREE plan")
    void findByName_free() {
        Optional<Plan> plan = planRepo.findByName("FREE");
        assertThat(plan).isPresent();
        assertThat(plan.get().getPriceMonthly()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("findByName – finds PRO plan with Stripe IDs")
    void findByName_pro() {
        Optional<Plan> plan = planRepo.findByName("PRO");
        assertThat(plan).isPresent();
        assertThat(plan.get().getStripePriceIdMonthly()).isEqualTo("price_pro_monthly");
        assertThat(plan.get().isHasAdvancedAnalytics()).isTrue();
    }

    @Test @DisplayName("findByName – finds BUSINESS plan with all features")
    void findByName_business() {
        Optional<Plan> plan = planRepo.findByName("BUSINESS");
        assertThat(plan).isPresent();
        assertThat(plan.get().isHasAutomation()).isTrue();
        assertThat(plan.get().isHasPrioritySupport()).isTrue();
        assertThat(plan.get().getMaxMembersPerWorkspace()).isEqualTo(-1);
    }

    @Test @DisplayName("findByName – empty for unknown plan")
    void findByName_notFound() {
        assertThat(planRepo.findByName("ENTERPRISE")).isEmpty();
    }

    @Test @DisplayName("findAll plans – returns all 3 seeded plans")
    void findAllPlans() {
        assertThat(planRepo.findAll()).hasSize(3);
    }

    @Test @DisplayName("plan save – assigns ID on persist")
    void planSave_assignsId() {
        Plan newPlan = planRepo.save(Plan.builder()
                .name("STARTER").displayName("Starter")
                .priceMonthly(new BigDecimal("4.99")).priceYearly(new BigDecimal("49.00"))
                .maxWorkspaces(10).maxBoardsPerWorkspace(20).maxMembersPerWorkspace(25)
                .hasAdvancedAnalytics(false).hasPrioritySupport(false)
                .hasCustomFields(false).hasAutomation(false).build());
        assertThat(newPlan.getId()).isNotNull();
    }

    // ── SubscriptionRepository ──────────────────────────────────────────────
    @Test @DisplayName("findByUserId – returns active subscription for user")
    void findByUserId_active() {
        Optional<Subscription> sub = subRepo.findByUserId(1L);
        assertThat(sub).isPresent();
        assertThat(sub.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(sub.get().getPlan().getName()).isEqualTo("PRO");
    }

    @Test @DisplayName("findByUserId – returns cancelled subscription")
    void findByUserId_cancelled() {
        Optional<Subscription> sub = subRepo.findByUserId(2L);
        assertThat(sub).isPresent();
        assertThat(sub.get().getStatus()).isEqualTo("CANCELLED");
    }

    @Test @DisplayName("findByUserId – empty for new user")
    void findByUserId_empty() {
        assertThat(subRepo.findByUserId(99L)).isEmpty();
    }

    @Test @DisplayName("findByStripeSubscriptionId – finds by Stripe sub ID")
    void findByStripeSubId() {
        Optional<Subscription> sub = subRepo.findByStripeSubscriptionId("sub_abc123");
        assertThat(sub).isPresent();
        assertThat(sub.get().getUserId()).isEqualTo(1L);
    }

    @Test @DisplayName("findByStripeSubscriptionId – empty for unknown sub ID")
    void findByStripeSubId_notFound() {
        assertThat(subRepo.findByStripeSubscriptionId("sub_unknown")).isEmpty();
    }

    @Test @DisplayName("findByStripeCustomerId – finds by Stripe customer ID")
    void findByStripeCustomerId() {
        Optional<Subscription> sub = subRepo.findByStripeCustomerId("cus_abc123");
        assertThat(sub).isPresent();
        assertThat(sub.get().getBillingCycle()).isEqualTo("MONTHLY");
    }

    @Test @DisplayName("findByStripeCustomerId – empty for unknown customer")
    void findByStripeCustomerId_notFound() {
        assertThat(subRepo.findByStripeCustomerId("cus_unknown")).isEmpty();
    }

    @Test @DisplayName("subscription save – updates status to PAST_DUE")
    void subSave_updatesStatus() {
        activeSub.setStatus("PAST_DUE");
        subRepo.save(activeSub);
        assertThat(subRepo.findById(activeSub.getId()).orElseThrow().getStatus())
                .isEqualTo("PAST_DUE");
    }

    @Test @DisplayName("subscription save – updates plan reference")
    void subSave_updatesPlan() {
        activeSub.setPlan(businessPlan);
        subRepo.save(activeSub);
        assertThat(subRepo.findById(activeSub.getId()).orElseThrow()
                .getPlan().getName()).isEqualTo("BUSINESS");
    }

    @Test @DisplayName("findAll subscriptions – returns both seeded subs")
    void findAllSubs() {
        assertThat(subRepo.findAll()).hasSize(2);
    }

    // ── PaymentRecordRepository ─────────────────────────────────────────────
    @Test @DisplayName("findByUserIdOrderByCreatedAtDesc – returns records newest first")
    void findByUserId_ordered() {
        paymentRepo.save(PaymentRecord.builder()
                .userId(1L).stripePaymentIntentId("pi_1")
                .amount(new BigDecimal("9.99")).currency("USD")
                .status("SUCCEEDED").description("Invoice inv_1")
                .createdAt(LocalDateTime.now().minusMonths(1)).build());

        paymentRepo.save(PaymentRecord.builder()
                .userId(1L).stripePaymentIntentId("pi_2")
                .amount(new BigDecimal("9.99")).currency("USD")
                .status("SUCCEEDED").description("Invoice inv_2")
                .createdAt(LocalDateTime.now()).build());

        List<PaymentRecord> records = paymentRepo.findByUserIdOrderByCreatedAtDesc(1L);
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getCreatedAt())
                .isAfterOrEqualTo(records.get(1).getCreatedAt());
    }

    @Test @DisplayName("findByUserIdOrderByCreatedAtDesc – empty for new user")
    void findByUserId_empty_noPaymentRecords() {
        assertThat(paymentRepo.findByUserIdOrderByCreatedAtDesc(99L)).isEmpty();
    }

    @Test @DisplayName("payment record save – assigns ID")
    void paymentSave_assignsId() {
        PaymentRecord rec = paymentRepo.save(PaymentRecord.builder()
                .userId(3L).stripePaymentIntentId("pi_new")
                .amount(new BigDecimal("29.99")).currency("USD")
                .status("SUCCEEDED").description("Invoice inv_new")
                .createdAt(LocalDateTime.now()).build());
        assertThat(rec.getId()).isNotNull();
    }

    @Test @DisplayName("payment record save – persists FAILED status")
    void paymentSave_failed() {
        PaymentRecord rec = paymentRepo.save(PaymentRecord.builder()
                .userId(1L).stripePaymentIntentId("pi_fail")
                .amount(new BigDecimal("9.99")).currency("USD")
                .status("FAILED").description("Invoice inv_fail")
                .createdAt(LocalDateTime.now()).build());
        assertThat(paymentRepo.findById(rec.getId()).orElseThrow().getStatus())
                .isEqualTo("FAILED");
    }

    @Test @DisplayName("payment record delete – removes record")
    void paymentDelete() {
        PaymentRecord rec = paymentRepo.save(PaymentRecord.builder()
                .userId(5L).stripePaymentIntentId("pi_del")
                .amount(BigDecimal.TEN).currency("USD").status("SUCCEEDED")
                .createdAt(LocalDateTime.now()).build());
        paymentRepo.deleteById(rec.getId());
        assertThat(paymentRepo.findById(rec.getId())).isEmpty();
    }
}