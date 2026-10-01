package com.seopulse.report;

import com.seopulse.report.render.ReportModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Unauthenticated, read-only access through share tokens and signed URLs. */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicReportController {

    private final ReportService reportService;

    @GetMapping("/reports/{token}")
    public ResponseEntity<ReportModel> sharedReport(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(reportService.sharedReport(token));
    }

    @GetMapping("/reports/{token}/pdf")
    public ResponseEntity<byte[]> sharedPdf(@PathVariable String token) {
        return pdf(reportService.sharedPdf(token), "seo-report.pdf");
    }

    @GetMapping("/report-files/{reportId}")
    public ResponseEntity<byte[]> signedFile(
            @PathVariable Long reportId,
            @RequestParam long expires,
            @RequestParam String sig
    ) {
        return pdf(reportService.signedFile(reportId, expires, sig), "seo-report-" + reportId + ".pdf");
    }

    private static ResponseEntity<byte[]> pdf(byte[] body, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
