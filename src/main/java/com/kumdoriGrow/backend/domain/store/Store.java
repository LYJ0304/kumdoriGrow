package com.kumdoriGrow.backend.domain.store;

import com.kumdoriGrow.backend.domain.category.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stores", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stores_normalized", columnNames = "normalized_name")
})
public class Store {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(name = "category_code", nullable = false, length = 20)
    private String categoryCode;

    @Column(length = 100)
    private String brand;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code", referencedColumnName = "code", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreAlias> aliases = new ArrayList<>();

    public Store(String name, String normalizedName, String categoryCode, String brand) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.categoryCode = categoryCode;
        this.brand = brand;
    }
}