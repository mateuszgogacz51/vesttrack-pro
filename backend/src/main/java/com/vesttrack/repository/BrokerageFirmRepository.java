package com.vesttrack.repository;

import com.vesttrack.domain.entity.BrokerageFirm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrokerageFirmRepository extends JpaRepository<BrokerageFirm, Long> {

    List<BrokerageFirm> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String query);

    List<BrokerageFirm> findByActiveTrueOrderByNameAsc();
}
