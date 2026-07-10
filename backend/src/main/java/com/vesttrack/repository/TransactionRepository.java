package com.vesttrack.repository;

import com.vesttrack.domain.entity.Transaction;
import com.vesttrack.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // kolejnosc chronologiczna - niezbedna dla algorytmu FIFO
    @Query("select t from Transaction t " +
           "where t.account.id = :accountId and t.instrument.id = :instrumentId " +
           "and t.transactionType = :type " +
           "order by t.transactionDate asc, t.id asc")
    List<Transaction> findChronological(@Param("accountId") Long accountId,
                                         @Param("instrumentId") Long instrumentId,
                                         @Param("type") TransactionType type);

    @Query("select t from Transaction t " +
           "join fetch t.instrument " +
           "where t.account.id = :accountId " +
           "order by t.transactionDate desc, t.id desc")
    List<Transaction> findByAccountIdWithInstrument(@Param("accountId") Long accountId);

    // agregacja do wykresu wartosci portfela w czasie (przyklad zapytania natywnego z window function)
    @Query(value = """
            SELECT
                t.transaction_date AS tx_date,
                SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.quantity * t.price * t.exchange_rate
                         WHEN t.transaction_type = 'SELL' THEN -t.quantity * t.price * t.exchange_rate
                         ELSE 0 END)
                    OVER (ORDER BY t.transaction_date ASC) AS cumulative_invested
            FROM transactions t
            WHERE t.account_id = :accountId
            ORDER BY t.transaction_date ASC
            """, nativeQuery = true)
    List<Object[]> findCumulativeInvestedSeries(@Param("accountId") Long accountId);
}
