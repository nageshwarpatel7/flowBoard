package com.flowboard.payment_service.repository;

import com.flowboard.payment_service.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
