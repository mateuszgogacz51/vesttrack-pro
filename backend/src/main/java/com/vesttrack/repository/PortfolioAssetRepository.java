package com.vesttrack.repository;

import com.vesttrack.domain.entity.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {

    @Query("select pa from PortfolioAsset pa " +
           "join fetch pa.instrument " +
           "where pa.account.id = :accountId")
    List<PortfolioAsset> findByAccountIdWithInstrument(@Param("accountId") Long accountId);

    Optional<PortfolioAsset> findByAccountIdAndInstrumentId(Long accountId, Long instrumentId);
}
