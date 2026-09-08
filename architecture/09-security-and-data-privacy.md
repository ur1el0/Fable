# 09 Security & Data Privacy Architecture

## 1. Threat Modeling for an Offline Reader

Although Fable is a client-side reader without a central user database, it interacts with untrusted external web novel servers and processes untrusted user-supplied or web-scraped content. The primary threat vectors are:

1. **Malicious Script / HTML Injection:** Web novel pages containing hidden `<script>`, `<style>`, `<iframe>`, or JavaScript event handlers designed to steal local data or exploit Android WebView / text rendering components.
2. **Directory Traversal (Path Traversal):** Maliciously crafted book titles or chapter IDs (e.g., `../../system/bin`) attempting to escape the app sandbox when saving files to disk.
3. **Tracking & Telemetry Exfiltration:** Third-party tracking libraries profiling user reading habits, frequency, or device identifiers.

---

## 2. Security Defense Implementations

### 2.1 Mandatory Jsoup HTML Sanitization
All raw content fetched by source extractors is filtered through a strict whitelist before disk writing or rendering:
- **Permitted Tags:** Structural formatting only (`<p>`, `<b>`, `<i>`, `<em>`, `<strong>`, `<br>`, `<h1>`-`<h6>`).
- **Forbidden Tags:** Strictly removed (`<script>`, `<style>`, `<iframe>`, `<embed>`, `<object>`, `<form>`, `<input>`).
- **Forbidden Attributes:** All inline script triggers (`onclick`, `onload`, `onerror`, `style`) are stripped.

### 2.2 Scoped Storage & Canonical Path Verification
All chapter text and cover assets reside strictly within `context.filesDir`. Path traversal is defended by verifying canonical paths:
```kotlin
fun assertSafePath(baseDir: File, targetFile: File) {
    if (!targetFile.canonicalPath.startsWith(baseDir.canonicalPath)) {
        throw SecurityException("Access denied: Path traversal attempt outside app sandbox.")
    }
}
```

### 2.3 Zero Telemetry & Offline Sovereignty
- Zero tracking or analytics SDKs are incorporated into Fable's dependencies.
- No network requests are made on startup unless explicitly initiated by the user (searching a source or refreshing a catalog).
- The Room database contains no remote identity tokens or advertiser IDs.

### 2.4 Android Network Security Configuration
Cleartext HTTP traffic is blocked by default across all application layers in `res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

