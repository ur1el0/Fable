# 05 Networking & Source Extractor Pipeline

## 1. Modular Source Extractor Architecture

Fable decouples external web content ingestion from the core application using a modular extractor interface. Every website scraper implements the unified `Source` contract:

```kotlin
interface Source {
    val id: String
    val name: String
    val baseUrl: String
    val iconUrl: String?

    suspend fun getPopularBooks(page: Int): List<Book>
    suspend fun searchBooks(query: String, page: Int): List<Book>
    suspend fun getBookDetails(bookUrl: String): Book
    suspend fun getChapterList(bookUrl: String): List<Chapter>
    suspend fun getChapterContent(chapterUrl: String): String
}
```

By isolating CSS selectors and URL query construction inside discrete extractor classes, external website redesigns or DOM changes can be repaired without touching the UI, Room database, or reader rendering code.

---

## 2. OkHttp Networking Pipeline

All network communication routes through a centralized, optimized OkHttp client:

```kotlin
class NetworkClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(request)
        }
        .build()
}
```

### Ethical Scraping & Rate Limiting
To prevent IP bans and respect web servers:
- Minimum delay of **500ms** enforced between consecutive chapter downloads.
- Automatic exponential backoff when encountering HTTP 429 (Too Many Requests).

---

## 3. Jsoup Content Extraction & Sanitization Pipeline

When an extractor fetches raw HTML for a chapter, it routes through a two-stage parsing and sanitization pipeline:

```
[Raw HTML Response] 
        │
        ▼
[Jsoup DOM Parsing] (CSS Selectors isolate chapter container)
        │
        ▼
[Jsoup Sanitization Filter] (Safelist strips scripts, iframes, styles, ads)
        │
        ▼
[Clean Structured HTML] (Stored in Scoped Storage sandbox)
```

### Strict Sanitizer Safelist
```kotlin
fun sanitizeChapterHtml(rawHtml: String): String {
    val safelist = Safelist.relaxed()
        .removeTags("script", "style", "iframe", "embed", "object", "form", "input", "button")
        .removeAttributes(":all", "onclick", "onload", "onerror", "style", "id", "class")
    return Jsoup.clean(rawHtml, safelist)
}
```

### Memory-Conscious DOM Traversal
- Jsoup `Document` objects are parsed strictly on `Dispatchers.Default`.
- The cleaned text or HTML string is extracted immediately, and the heavy DOM tree is allowed to be garbage collected to prevent memory bloat.

