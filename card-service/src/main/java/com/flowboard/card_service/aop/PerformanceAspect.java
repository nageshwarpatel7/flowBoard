package com.flowboard.card_service.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // Warn if any service method takes longer than 500 ms
    private static final long SLOW_THRESHOLD_MS = 500;

    @Around("execution(* com.flowboard.card_service.service.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > SLOW_THRESHOLD_MS) {
            log.warn("[SLOW] {} took {}ms (threshold={}ms)",
                    pjp.getSignature().toShortString(), elapsed, SLOW_THRESHOLD_MS);
        }
        return result;
    }
}