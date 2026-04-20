package com.flowboard.card_service.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Pointcut: every public method in the service layer
    @Pointcut("execution(* com.flowboard.card_service.service.*.*(..))")
    public void serviceLayer() {}

    // Pointcut: every public method in the controller layer
    @Pointcut("execution(* com.flowboard.card_service.controller.*.*(..))")
    public void controllerLayer() {}

    /**
     * Logs method entry + args, exit + result, and execution time.
     * Applied to every service method.
     */
    @Around("serviceLayer()")
    public Object logServiceCall(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();

        log.debug("[ENTRY] {} args={}", method, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[EXIT]  {} returned={} time={}ms", method, result, elapsed);
            return result;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[ERROR] {} threw {} after {}ms — {}",
                    method, ex.getClass().getSimpleName(), elapsed, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Logs every incoming REST request (method + args).
     * Applied to every controller method.
     */
    @Before("controllerLayer()")
    public void logRequest(JoinPoint jp) {
        log.info("[REQUEST] {} args={}",
                jp.getSignature().toShortString(),
                Arrays.toString(jp.getArgs()));
    }

    /**
     * Logs when a service method completes without exception.
     */
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logSuccess(JoinPoint jp, Object result) {
        log.debug("[SUCCESS] {}", jp.getSignature().getName());
    }

    /**
     * Logs every exception thrown from the service layer.
     */
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint jp, Exception ex) {
        log.error("[EXCEPTION] {} threw {}: {}",
                jp.getSignature().toShortString(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}