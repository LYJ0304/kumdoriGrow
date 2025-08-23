package com.kumdoriGrow.backend.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreAliasRepository extends JpaRepository<StoreAlias, Long> {
    
    Optional<StoreAlias> findByNormalizedAlias(String normalizedAlias);
    
    @Query("SELECT sa FROM StoreAlias sa WHERE sa.normalizedAlias LIKE %:normalizedAlias%")
    List<StoreAlias> findByNormalizedAliasContaining(String normalizedAlias);
    
    List<StoreAlias> findByStoreId(Long storeId);
}