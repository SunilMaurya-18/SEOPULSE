package com.seopulse.notification;

import java.util.Map;

public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static String html(String title, String intro, String actionLabel, String actionUrl) {
        String button = actionUrl == null ? "" : """
                <p style="margin:28px 0">
                  <a href="%s" style="background:#f5504a;color:#1c0d0b;text-decoration:none;padding:12px 18px;border-radius:8px;font-family:IBM Plex Mono,monospace;font-size:14px">%s</a>
                </p>
                <p style="color:#8b93a1;font-size:13px;word-break:break-all">%s</p>
                """.formatted(actionUrl, actionLabel, actionUrl);
        return """
                <div style="background:#05070a;padding:32px;color:#f2f5ea;font-family:IBM Plex Mono,ui-monospace,monospace">
                  <p style="letter-spacing:.18em;text-transform:uppercase;color:#f5504a;font-size:12px">SEOPulse</p>
                  <h1 style="font-weight:400;font-size:28px;margin:12px 0">%s</h1>
                  <p style="color:#c5c9c2;line-height:1.6;font-size:15px">%s</p>
                  %s
                </div>
                """.formatted(escape(title), escape(intro), button);
    }

    public static String text(String intro, String url) {
        return url == null ? intro : intro + "\n\n" + url;
    }

    public static String fill(String template, Map<String, String> values) {
        String result = template;
        for (var entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
