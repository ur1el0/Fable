# ADR 005: OkHttp & Jsoup Content Scraping Pipeline

## Status
Accepted

## Context
Many mobile readers rely on hidden Android `WebView` instances to load remote chapters and extract content via JavaScript evaluation. However, WebViews incur massive memory overhead (often 80MB–150MB per instance), cause severe UI stutter, and introduce significant security risks from uncontained script execution.

## Decision
We chose a headless networking and parsing pipeline combining **OkHttp** and **Jsoup**:
- **OkHttp:** Manages low-level HTTP/2 and TLS connections, realistic browser header emulation, connection pooling, and rate-limiting.
- **Jsoup:** Parses HTML into a structured DOM tree, enables precise CSS selector querying (`doc.select("div.chapter-content > p")`), and applies strict `Safelist` sanitization to strip all `<script>`, `<style>`, `<iframe>`, and event-handling attributes.

## Consequences
- **Positive:** Negligible memory footprint (<10MB per extraction cycle) compared to WebViews.
- **Positive:** Total immunity from arbitrary JavaScript execution and web tracking pixels embedded in source chapters.
- **Negative:** Sources requiring complex dynamic JavaScript rendering (e.g., Cloudflare Turnstile captchas or Single Page Apps) cannot be parsed with simple GET requests and require specialized extractor strategies.
