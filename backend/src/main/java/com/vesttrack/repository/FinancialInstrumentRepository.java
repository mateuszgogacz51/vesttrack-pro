package com.vesttrack.repository;

import com.vesttrack.domain.entity.FinancialInstrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialInstrumentRepository extends JpaRepository<FinancialInstrument, Long> {
    Optional<FinancialInstrument> findByTickerAndExchange(String ticker, String exchange);
    List<FinancialInstrument> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(String ticker, String name);
    Optional<FinancialInstrument> findByIsin(String isin);
}
