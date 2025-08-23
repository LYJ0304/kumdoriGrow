package com.kumdoriGrow.backend.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    
    Optional<Store> findByNormalizedName(String normalizedName);
    
    @Query("SELECT s FROM Store s WHERE s.normalizedName LIKE %:normalizedName%")
    List<Store> findByNormalizedNameContaining(String normalizedName);
    
    List<Store> findByCategoryCode(String categoryCode);
}