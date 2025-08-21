package com.kumdoriGrow.backend.domain.receipt;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipts_user_created", columnList = "user_id,created_at")
})
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "category_code", nullable = false, length = 20)
    private String categoryCode; // FRANCHISE/LOCAL/MARKET

    @Column(name = "exp_awarded", nullable = false)
    private Integer expAwarded;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    // MySQL JSON 컬럼. (MariaDB 구버전이면 columnDefinition 제거하고 LONGTEXT로 바꿔도 됨)
    @Column(name = "ocr_raw", columnDefinition = "json")
    private String ocrRaw;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DONE";

    @Column(name = "recognized_at")
    private Instant recognizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // --- getter/setter ---
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Integer getExpAwarded() { return expAwarded; }
    public void setExpAwarded(Integer expAwarded) { this.expAwarded = expAwarded; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getOcrRaw() { return ocrRaw; }
    public void setOcrRaw(String ocrRaw) { this.ocrRaw = ocrRaw; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getRecognizedAt() { return recognizedAt; }
    public void setRecognizedAt(Instant recognizedAt) { this.recognizedAt = recognizedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
