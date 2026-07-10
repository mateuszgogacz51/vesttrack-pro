package com.vesttrack.repository;

import com.vesttrack.domain.entity.InvestmentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, Long> {

    List<InvestmentAccount> findByUserId(Long userId);

    @Query("select a from InvestmentAccount a where a.id = :id and a.user.id = :userId")
    Optional<InvestmentAccount> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
