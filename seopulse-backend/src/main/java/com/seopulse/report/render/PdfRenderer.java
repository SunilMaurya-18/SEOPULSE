package com.seopulse.report.render;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiPredicate;

/**
 * Converts report HTML to PDF. Only inline {@code data:} resources are
 * loaded; any other URL (http, file, jar...) is refused, so report content
 * can never make the server fetch something.
 */
@Component
public class PdfRenderer {

    private static final BiPredicate<String, ExternalResourceType> ONLY_DATA_URIS =
            (uri, type) -> uri != null && uri.startsWith("data:");

    public byte[] render(String html) throws IOException {

        org.w3c.dom.Document document = new W3CDom().fromJsoup(Jsoup.parse(html));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withW3cDocument(document, "about:blank");
        builder.useUriResolver((baseUri, uri) -> uri != null && uri.startsWith("data:") ? uri : null);
        builder.useExternalResourceAccessControl(ONLY_DATA_URIS, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);
        builder.useExternalResourceAccessControl(ONLY_DATA_URIS, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
        builder.toStream(out);
        builder.run();

        return out.toByteArray();
    }
}
