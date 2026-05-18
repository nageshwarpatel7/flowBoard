package com.flowboard.payment_service.config;

import com.flowboard.payment_service.entity.Plan;
import com.flowboard.payment_service.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) return;

        planRepository.save(Plan.builder()
                .name("FREE").displayName("Free").description("Get started for free")
                .priceMonthly(BigDecimal.ZERO).priceYearly(BigDecimal.ZERO)
                .maxWorkspaces(3).maxBoardsPerWorkspace(5).maxMembersPerWorkspace(10)
                .hasAdvancedAnalytics(false).hasPrioritySupport(false)
                .hasCustomFields(false).hasAutomation(false)
                .build());

        planRepository.save(Plan.builder()
                .name("PRO").displayName("Pro").description("For growing teams")
                .priceMonthly(new BigDecimal("9.99")).priceYearly(new BigDecimal("99.00"))
                .stripePriceIdMonthly("price_1TODqvANErF8WTmGRzRIjKTG")
                .stripePriceIdYearly("price_1TODuYANErF8WTmG9Ce6IDKb")
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).maxMembersPerWorkspace(50)
                .hasAdvancedAnalytics(true).hasPrioritySupport(false)
                .hasCustomFields(true).hasAutomation(false)
                .build());

        planRepository.save(Plan.builder()
                .name("BUSINESS").displayName("Business").description("For enterprises")
                .priceMonthly(new BigDecimal("29.99")).priceYearly(new BigDecimal("299.00"))
                .stripePriceIdMonthly("prod_UMxnb56O3fTvQw")
                .stripePriceIdYearly("price_1TODtdANErF8WTmG8sLj6zxC")
                .maxWorkspaces(-1).maxBoardsPerWorkspace(-1).maxMembersPerWorkspace(-1)
                .hasAdvancedAnalytics(true).hasPrioritySupport(true)
                .hasCustomFields(true).hasAutomation(true)
                .build());

        log.info("Plans seeded: FREE, PRO, BUSINESS");
    }
}
