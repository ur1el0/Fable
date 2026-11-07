---
trigger: always_on
---

# Reader Engine & Source Extraction Guidelines

These rules govern the implementation of web scraping extractors, HTML sanitization, and the Compose text rendering engine in Fable.

---

### 1. Modular Source Extractor Contract
- **Rule**: Every content scraper must implement the unified `Source` interface.
- **Practice**:
  - Never write ad-hoc network scraping logic inside UI components, ViewModels, or general repositories.
  - Implement the contract:
    ```kotlin
    interface Source {
        val id: String
        val name: String
        val baseUrl: String
        suspend fun searchBooks(query: String, page: Int): List<Book>
        suspend fun getPopularBooks(page: Int): List<Book>
        suspend fun getChapterList(bookUrl: String): List<Chapter>
        suspend fun getChapterContent(chapterUrl: String): String
    }
    ```
  - Isolate site-specific CSS selectors inside the respective source implementation.

---

### 2. Mandatory Jsoup Content Sanitization
- **Rule**: All scraped HTML must pass through a strict sanitization filter before disk storage or UI rendering.
- **Practice**:
  - Use Jsoup `Cleaner` with a strict `Safelist`:
    ```kotlin
    fun sanitizeHtml(rawHtml: String): String {
        val safelist = Safelist.relaxed()
            .removeTags("script", "style", "iframe", "embed", "object", "form")
            .removeAttributes(":all", "onclick", "onload", "onerror", "style")
        return Jsoup.clean(rawHtml, safelist)
    }
    ```
  - Never inject raw, unsanitized external HTML into Android WebViews or custom text layouts.

---

### 3. Memory-Conscious DOM Traversal
- **Rule**: Never retain Jsoup `Document` or `Element` instances in long-lived memory or ViewModels.
- **Practice**:
  - Parse the DOM inside a local coroutine scope on `Dispatchers.IO`.
  - Extract the required text, clean paragraphs, or structured chapter models immediately, and allow the heavy DOM tree to be garbage collected.

---

### 4. Reader Layout & Progress Persistence
- **Rule**: Reading progress must be accurately calculated and saved without locking the UI thread.
- **Practice**:
  - In **Paging Mode**, track `currentPageIndex` and `totalPages`.
  - In **Continuous Scroll Mode**, track `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`.
  - Debounce progress saves to the Room database (minimum 500ms debounce) to avoid disk thrashing during active rapid scrolling.
  - Automatically restore exact scroll position when opening a chapter.

---

### 5. Ethical Scraping & Network Hygiene
- **Rule**: Respect source web servers and prevent IP blocks.
- **Practice**:
  - Set realistic desktop/mobile User-Agent headers on all OkHttp requests.
  - Enforce a minimum delay (500ms) between consecutive chapter extraction requests.
  - Handle HTTP 429 (Too Many Requests) and HTTP 503 gracefully by returning structured domain error results.

