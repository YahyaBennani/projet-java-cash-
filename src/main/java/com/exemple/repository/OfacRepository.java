package com.exemple.repository;

import com.exemple.entity.OfacEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfacRepository extends JpaRepository<OfacEntry, Long> {
    boolean existsByNomContainingIgnoreCaseAndActifTrue(String nom);
}
