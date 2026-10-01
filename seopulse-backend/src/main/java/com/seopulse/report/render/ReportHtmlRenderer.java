package com.seopulse.report.render;

import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Builds the print layout for a report. Every dynamic value is HTML-escaped. */
@Component
public class ReportHtmlRenderer {

    static final String DEFAULT_COLOR = "#f5504a";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern LOGO = Pattern.compile("^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    public String render(ReportModel model) {

        ReportModel.Branding branding = model.branding();
        String color = branding != null && branding.brandColor() != null && HEX_COLOR.matcher(branding.brandColor()).matches()
                ? branding.brandColor()
                : DEFAULT_COLOR;
        String brandName = branding != null && notBlank(branding.companyName()) ? branding.companyName() : "SEOPulse";

        StringBuilder html = new StringBuilder(16_384);
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><title>")
                .append(esc("SEO report - " + model.websiteName()))
                .append("</title><style>").append(css(color)).append("</style></head><body>");

        if (model.watermark()) {
            html.append("<div class=\"watermark\">SEOPulse Free</div>");
        }

        // Cover
        html.append("<div class=\"cover\">");
        if (branding != null && branding.logoDataUrl() != null && LOGO.matcher(branding.logoDataUrl()).matches()) {
            html.append("<img class=\"logo\" alt=\"\" src=\"").append(branding.logoDataUrl()).append("\"/>");
        }
        html.append("<p class=\"eyebrow\">").append(esc(brandName)).append(" SEO report</p>")
                .append("<h1>").append(esc(model.websiteName())).append("</h1>")
                .append("<p class=\"muted\">").append(esc(model.websiteUrl())).append("</p>");
        if (model.completedAt() != null) {
            html.append("<p class=\"muted\">Audit completed ").append(esc(DATE.format(model.completedAt()))).append("</p>");
        }
        if (branding != null && notBlank(branding.coverText())) {
            html.append("<p class=\"cover-text\">").append(esc(branding.coverText())).append("</p>");
        }
        html.append("</div>");

        // Summary
        html.append("<table class=\"summary\"><tr>")
                .append(stat("Score", model.score() == null ? "-" : String.valueOf(model.score()), scoreDelta(model)))
                .append(stat("Pages crawled", String.valueOf(model.pagesCrawled()), null))
                .append(stat("Errors", String.valueOf(model.errorCount()), null))
                .append(stat("Warnings", String.valueOf(model.warningCount()), null))
                .append(stat("Notices", String.valueOf(model.infoCount()), null))
                .append("</tr></table>");

        if (model.newIssues() != null) {
            html.append("<p class=\"changes\">Since the previous audit: <b>")
                    .append(model.newIssues()).append(" new</b> and <b>")
                    .append(model.fixedIssues()).append(" fixed</b> issues.</p>");
        }

        // Category scores
        if (model.categoryScores() != null && !model.categoryScores().isEmpty()) {
            html.append("<h2>Scores by category</h2><table class=\"grid\"><tr><th>Category</th><th class=\"num\">Score</th></tr>");
            for (Map.Entry<String, Integer> entry : model.categoryScores().entrySet()) {
                html.append("<tr><td>").append(esc(titleCase(entry.getKey()))).append("</td><td class=\"num\">")
                        .append(entry.getValue()).append("</td></tr>");
            }
            html.append("</table>");
        }

        // Issues
        html.append("<h2>Top issues</h2>");
        if (!model.detailsAvailable()) {
            html.append("<p class=\"muted\">Issue details for this audit have been removed under the plan's data retention policy.</p>");
        } else if (model.topIssues().isEmpty()) {
            html.append("<p>No issues found. Nice work.</p>");
        } else {
            for (ReportModel.RuleGroup group : model.topIssues()) {
                html.append("<div class=\"issue\"><p class=\"issue-title\"><span class=\"sev sev-")
                        .append(esc(group.severity() == null ? "info" : group.severity().toLowerCase(Locale.ROOT)))
                        .append("\">").append(esc(group.severity())).append("</span> ")
                        .append(esc(group.title())).append(" <span class=\"muted\">(")
                        .append(group.count()).append(group.count() == 1 ? " page" : " pages").append(")</span></p>");
                if (notBlank(group.recommendation())) {
                    html.append("<p class=\"rec\">").append(esc(group.recommendation())).append("</p>");
                }
                html.append("<ul>");
                for (String url : group.sampleUrls()) {
                    html.append("<li>").append(esc(url)).append("</li>");
                }
                html.append("</ul></div>");
            }
        }

        html.append("<p class=\"footer\">");
        html.append(branding == null ? "Generated by SEOPulse" : "Prepared by " + esc(brandName));
        html.append("</p></body></html>");
        return html.toString();
    }

    private static String stat(String label, String value, String note) {
        return "<td><p class=\"stat-label\">" + esc(label) + "</p><p class=\"stat-value\">" + esc(value) + "</p>"
                + (note == null ? "" : "<p class=\"stat-note\">" + esc(note) + "</p>") + "</td>";
    }

    private static String scoreDelta(ReportModel model) {
        if (model.previousScore() == null || model.score() == null) {
            return null;
        }
        int delta = model.score() - model.previousScore();
        return (delta > 0 ? "+" : "") + delta + " vs previous";
    }

    private static String css(String color) {
        return """
                @page { size: A4; margin: 18mm 16mm 20mm 16mm; }
                body { font-family: Helvetica, sans-serif; color: #1d232b; font-size: 10.5pt; line-height: 1.45; }
                .cover { border-left: 6px solid %1$s; padding: 4mm 0 4mm 6mm; margin-bottom: 8mm; }
                .logo { max-height: 18mm; max-width: 60mm; margin-bottom: 4mm; }
                .eyebrow { color: %1$s; text-transform: uppercase; letter-spacing: 2px; font-size: 9pt; margin: 0; }
                h1 { font-size: 24pt; font-weight: normal; margin: 2mm 0; }
                h2 { font-size: 14pt; font-weight: normal; border-bottom: 1px solid #d9dde3; padding-bottom: 2mm; margin-top: 9mm; }
                .muted { color: #6b7380; margin: 1mm 0; }
                .cover-text { margin-top: 4mm; }
                table.summary { width: 100%%; border-collapse: collapse; margin: 4mm 0; }
                table.summary td { border: 1px solid #d9dde3; padding: 3mm; vertical-align: top; width: 20%%; }
                .stat-label { margin: 0; color: #6b7380; font-size: 8.5pt; text-transform: uppercase; }
                .stat-value { margin: 1mm 0 0 0; font-size: 18pt; }
                .stat-note { margin: 0; font-size: 8.5pt; color: #6b7380; }
                .changes { margin: 2mm 0 0 0; }
                table.grid { width: 60%%; border-collapse: collapse; }
                table.grid th, table.grid td { text-align: left; padding: 1.5mm 2mm; border-bottom: 1px solid #e5e8ec; }
                .num { text-align: right; }
                .issue { margin: 0 0 5mm 0; page-break-inside: avoid; }
                .issue-title { margin: 0; font-weight: bold; }
                .rec { margin: 1mm 0; color: #3a424d; }
                ul { margin: 1mm 0 0 0; padding-left: 5mm; color: #3a424d; font-size: 9pt; }
                li { word-wrap: break-word; }
                .sev { font-size: 7.5pt; padding: 0.5mm 1.5mm; border-radius: 2px; color: #ffffff; }
                .sev-error { background: #c8372f; }
                .sev-warning { background: #b7791f; }
                .sev-info { background: #4a6fa5; }
                .footer { margin-top: 10mm; color: #8b93a1; font-size: 8.5pt; }
                .watermark { position: fixed; top: 110mm; left: 10mm; width: 180mm; text-align: center;
                             font-size: 54pt; color: #000000; opacity: 0.07; transform: rotate(-30deg); }
                """.formatted(color);
    }

    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
