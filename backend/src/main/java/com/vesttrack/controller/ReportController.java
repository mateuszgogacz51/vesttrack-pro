package com.vesttrack.controller;

import com.vesttrack.service.ReportExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Raporty", description = "Eksport transakcji do CSV oraz rocznego podsumowania PDF (pomoc przy PIT-38)")
@PreAuthorize("hasRole('USER')")
public class ReportController {

    private final ReportExportService reportExportService;

    @GetMapping("/account/{accountId}/transactions.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long accountId,
                                             @RequestParam(required = false) Integer year) {
        byte[] csv = reportExportService.exportTransactionsAsCsv(accountId, year);
        String filename = "transakcje_konto_" + accountId + (year != null ? "_" + year : "") + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(csv);
    }

    @GetMapping("/account/{accountId}/annual-summary.pdf")
    public ResponseEntity<byte[]> exportAnnualPdf(@PathVariable Long accountId,
                                                   @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : Year.now().getValue();
        byte[] pdf = reportExportService.exportAnnualPdfSummary(accountId, effectiveYear);
        String filename = "roczne_zestawienie_konto_" + accountId + "_" + effectiveYear + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
