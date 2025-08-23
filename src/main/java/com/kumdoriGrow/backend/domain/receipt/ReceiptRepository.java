package com.kumdoriGrow.backend.domain.receipt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    @Query("select coalesce(sum(r.expAwarded), 0) from Receipt r where r.userId = :userId")
    long sumExpByUser(@Param("userId") Long userId);

    Page<Receipt> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 보상 관련 조회 메서드들
    List<Receipt> findByUserIdAndStatus(Long userId, String status);
    
    long countByUserIdAndStatus(Long userId, String status);
    
    Page<Receipt> findByUserIdAndStatusOrderByRecognizedAtDesc(Long userId, String status, Pageable pageable);
    
    boolean existsByUserIdAndStatusAndOcrRawContaining(Long userId, String status, String ocrRawContent);
}
