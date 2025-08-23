package com.kumdoriGrow.backend.domain.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_aliases", uniqueConstraints = {
    @UniqueConstraint(name = "uk_alias_normalized", columnNames = "normalized_alias")
})
public class StoreAlias {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 255)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, length = 255)
    private String normalizedAlias;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    private Store store;

    public StoreAlias(Long storeId, String alias, String normalizedAlias) {
        this.storeId = storeId;
        this.alias = alias;
        this.normalizedAlias = normalizedAlias;
    }
}