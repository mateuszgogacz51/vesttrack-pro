package com.vesttrack.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.Transaction;
import com.vesttrack.domain.enums.TransactionType;
import com.vesttrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Generuje eksporty transakcji dla danego konta:
 *  - CSV: pelna lista transakcji, gotowa do dalszej obrobki w Excelu / do ksiegowego.
 *  - PDF: estetyczne, roczne zestawienie zrealizowanych zyskow/strat -
 *    baza do samodzielnego rozliczenia podatku (formularz PIT-38 w Polsce,
 *    podatek od dochodow kapitalowych - "podatek Belki", 19%).
 *
 * Uzywa biblioteki OpenPDF (fork iText, licencja LGPL/MPL - bezpieczna do uzycia komercyjnego).
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportTransactionsAsCsv(Long accountId, Integer year) {
        InvestmentAccount account = accountService.getOwnedAccountOrThrow(accountId);
        List<Transaction> transactions = filterByYear(
                transactionRepository.findByAccountIdWithInstrument(accountId), year);

        StringBuilder csv = new StringBuilder();
        csv.append("Data transakcji;Ticker;Typ;Ilosc;Cena;Waluta;Kurs wymiany;Prowizja;Zysk zrealizowany;Waluta zysku\n");

        for (Transaction t : transactions) {
            csv.append(t.getTransactionDate().format(DATE_FMT)).append(';')
               .append(t.getInstrument().getTicker()).append(';')
               .append(t.getTransactionType()).append(';')
               .append(t.getQuantity()).append(';')
               .append(t.getPrice()).append(';')
               .append(t.getInstrumentCurrency()).append(';')
               .append(t.getExchangeRate()).append(';')
               .append(t.getFee()).append(';')
               .append(t.getRealizedGain() != null ? t.getRealizedGain() : "").append(';')
               .append(t.getRealizedGainCurrency() != null ? t.getRealizedGainCurrency() : "")
               .append('\n');
        }

        // BOM UTF-8, aby Excel prawidlowo wyswietlil polskie znaki
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    public byte[] exportAnnualPdfSummary(Long accountId, int year) {
        InvestmentAccount account = accountService.getOwnedAccountOrThrow(accountId);
        List<Transaction> transactions = filterByYear(
                transactionRepository.findByAccountIdWithInstrument(accountId), year);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            document.add(new Paragraph("VestTrack Pro - Roczne zestawienie transakcji", titleFont));
            document.add(new Paragraph("Konto: " + account.getName() + " (" + account.getAccountType() + ")", normalFont));
            document.add(new Paragraph("Rok podatkowy: " + year, normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            String[] headers = {"Data", "Ticker", "Typ", "Ilosc", "Cena", "Zysk zrealizowany (PLN)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBorder(Rectangle.BOTTOM);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            BigDecimal totalRealizedGain = BigDecimal.ZERO;
            for (Transaction t : transactions) {
                table.addCell(new Phrase(t.getTransactionDate().format(DATE_FMT), normalFont));
                table.addCell(new Phrase(t.getInstrument().getTicker(), normalFont));
                table.addCell(new Phrase(t.getTransactionType().name(), normalFont));
                table.addCell(new Phrase(t.getQuantity().toPlainString(), normalFont));
                table.addCell(new Phrase(t.getPrice().toPlainString(), normalFont));
                String gainStr = t.getRealizedGain() != null ? t.getRealizedGain().toPlainString() : "-";
                table.addCell(new Phrase(gainStr, normalFont));
                if (t.getRealizedGain() != null) {
                    totalRealizedGain = totalRealizedGain.add(t.getRealizedGain());
                }
            }
            document.add(table);
            document.add(new Paragraph(" "));

            BigDecimal taxDue = totalRealizedGain.compareTo(BigDecimal.ZERO) > 0
                    ? totalRealizedGain.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            document.add(new Paragraph("Suma zrealizowanych zyskow/strat: " + totalRealizedGain + " PLN",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            document.add(new Paragraph(
                    "Szacowany podatek od dochodow kapitalowych (19%, tzw. podatek Belki): " + taxDue + " PLN",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Uwaga: powyzsze zestawienie ma charakter informacyjny i pomocniczy. " +
                    "Nie zastepuje profesjonalnego rozliczenia podatkowego (formularz PIT-38). " +
                    "Skonsultuj sie z ksiegowym lub doradca podatkowym przed zlozeniem deklaracji.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Blad generowania raportu PDF", e);
        }
    }

    private List<Transaction> filterByYear(List<Transaction> transactions, Integer year) {
        if (year == null) {
            return transactions;
        }
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        return transactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(from) && !t.getTransactionDate().isAfter(to))
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();
    }
}
