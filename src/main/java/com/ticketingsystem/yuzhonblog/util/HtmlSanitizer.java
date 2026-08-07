package com.ticketingsystem.yuzhonblog.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Server-side HTML sanitization to prevent stored XSS.
 * Uses jsoup Safelist to strip dangerous HTML while preserving safe content.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist BLOG_SAFE = Safelist.relaxed()
            // Allow iframes for embedded videos (Bilibili, YouTube)
            .addTags("iframe")
            .addAttributes("iframe", "src", "width", "height", "frameborder",
                    "allowfullscreen", "scrolling", "allow", "style")
            // Allow style attribute on common elements for card styling
            .addAttributes("img", "loading", "decoding")
            .addAttributes("div", "style", "class")
            .addAttributes("span", "style", "class")
            .addAttributes("p", "style", "class")
            .addAttributes("pre", "class")
            .addAttributes("code", "class")
            // Enforce safe link targets
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("iframe", "src", "https")
            .preserveRelativeLinks(false);

    /**
     * Sanitize HTML content for safe storage.
     * Strips dangerous elements (script, event handlers, javascript: URIs)
     * while preserving safe formatting and embedded content.
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, BLOG_SAFE);
    }
}
