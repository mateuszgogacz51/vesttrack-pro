package com.vesttrack.service;

import com.vesttrack.domain.entity.Transaction;
import com.vesttrack.domain.enums.TransactionType;
import com.vesttrack.exception.BusinessRuleException;
import com.vesttrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Implementacja metody FIFO (First In, First Out) do rozliczania zrealizowanych
 * zyskow/strat kapitalowych.
 *
 * Zasada dzialania: kazda transakcja BUY tworzy "lot" (partie jednostek z konkretna
 * cena zakupu), ktory trafia na koniec kolejki. Transakcja SELL zdejmuje jednostki
 * z poczatku kolejki (najstarsze lots), licząc realny zysk/strate jako:
 *   (cena sprzedazy - cena zakupu lota) * ilosc - proporcjonalna czesc prowizji.
 *
 * Struktura danych: Deque<Lot> - klasyczna implementacja kolejki FIFO,
 * O(1) dla operacji na poczatku/koncu kolejki.
 */
@Service
@RequiredArgsConstructor
public class FifoCalculationService {

    private final TransactionRepository transactionRepository;

    /**
     * Oblicza zrealizowany zysk/strate dla nowo rejestrowanej transakcji SELL,
     * na podstawie historii wczesniejszych transakcji BUY dla danego instrumentu na danym koncie.
     * Nie modyfikuje wczesniejszych transakcji - jedynie "konsumuje" dostepne lots w pamieci.
     */
    public BigDecimal calculateRealizedGain(Long accountId, Long instrumentId,
                                             BigDecimal sellQuantity, BigDecimal sellPrice,
                                             BigDecimal sellFee, BigDecimal sellExchangeRate) {

        Deque<Lot> lots = buildFifoQueue(accountId, instrumentId);

        BigDecimal remainingToSell = sellQuantity;
        BigDecimal totalCostBasis = BigDecimal.ZERO;

        while (remainingToSell.compareTo(BigDecimal.ZERO) > 0) {
            Lot oldestLot = lots.peekFirst();
            if (oldestLot == null) {
                throw new BusinessRuleException(
                        "Nie mozna sprzedac wiecej jednostek niz posiadane na koncie (brak wystarczajacej historii zakupow FIFO)");
            }

            BigDecimal consumedQty = oldestLot.quantity.min(remainingToSell);
            BigDecimal costBasisForConsumed = oldestLot.unitCostInBaseCurrency.multiply(consumedQty);
            totalCostBasis = totalCostBasis.add(costBasisForConsumed);

            oldestLot.quantity = oldestLot.quantity.subtract(consumedQty);
            remainingToSell = remainingToSell.subtract(consumedQty);

            if (oldestLot.quantity.compareTo(BigDecimal.ZERO) == 0) {
                lots.pollFirst();
            }
        }

        BigDecimal sellProceedsInBaseCurrency = sellQuantity.multiply(sellPrice).multiply(sellExchangeRate)
                .subtract(sellFee.multiply(sellExchangeRate));

        return sellProceedsInBaseCurrency.subtract(totalCostBasis).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Buduje kolejke FIFO z istniejacych juz w bazie transakcji BUY i SELL
     * (chronologicznie), tak aby kolejna transakcja SELL "widziala" prawidlowy
     * stan dostepnych lots.
     */
    private Deque<Lot> buildFifoQueue(Long accountId, Long instrumentId) {
        List<Transaction> buys = transactionRepository.findChronological(accountId, instrumentId, TransactionType.BUY);
        List<Transaction> sells = transactionRepository.findChronological(accountId, instrumentId, TransactionType.SELL);

        Deque<Lot> lots = new ArrayDeque<>();
        for (Transaction buy : buys) {
            BigDecimal unitCost = buy.getPrice()
                    .add(buy.getFee().divide(buy.getQuantity(), 10, RoundingMode.HALF_UP))
                    .multiply(buy.getExchangeRate());
            lots.addLast(new Lot(buy.getQuantity(), unitCost));
        }

        // "odtwarzamy" historie - konsumujemy lots zuzyte przez wczesniejsze, juz zaksiegowane sprzedaze
        for (Transaction sell : sells) {
            BigDecimal remaining = sell.getQuantity();
            while (remaining.compareTo(BigDecimal.ZERO) > 0 && !lots.isEmpty()) {
                Lot lot = lots.peekFirst();
                BigDecimal consumed = lot.quantity.min(remaining);
                lot.quantity = lot.quantity.subtract(consumed);
                remaining = remaining.subtract(consumed);
                if (lot.quantity.compareTo(BigDecimal.ZERO) == 0) {
                    lots.pollFirst();
                }
            }
        }

        return lots;
    }

    /** Pojedyncza partia (transza) zakupionych jednostek instrumentu, oczekujaca w kolejce FIFO. */
    private static class Lot {
        BigDecimal quantity;
        final BigDecimal unitCostInBaseCurrency;

        Lot(BigDecimal quantity, BigDecimal unitCostInBaseCurrency) {
            this.quantity = quantity;
            this.unitCostInBaseCurrency = unitCostInBaseCurrency;
        }
    }
}
