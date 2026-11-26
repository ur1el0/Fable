package com.example.fable.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ServerUrlValidator {
    const val HOSTED_RENDER_BASE_URL = "https://fable-api-cbo8.onrender.com/api/v1/"
    const val EMULATOR_LOCAL_BASE_URL = "http://10.0.2.2:8001/api/v1/"

    fun validateAndNormalize(rawUrl: String, allowCleartext: Boolean): Result<String> = runCatching {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotEmpty()) { "Enter a server URL." }

        val parsed = trimmed.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid http or https server URL.")

        if (parsed.scheme == "http" && !allowCleartext) {
            throw IllegalArgumentException("Only HTTPS server URLs are allowed.")
        }
        require(parsed.host.isNotBlank()) { "Enter a server URL with a valid host." }
        require(parsed.username.isEmpty() && parsed.password.isEmpty()) {
            "Server URL must not include credentials."
        }
        require(parsed.query == null && parsed.fragment == null) {
            "Server URL must not include query parameters or fragments."
        }

        val trimmedPath = parsed.encodedPath.trimEnd('/')
        val normalizedPath = when {
            trimmedPath.isEmpty() -> "/api/v1/"
            trimmedPath.endsWith("/api/v1") -> "$trimmedPath/"
            else -> throw IllegalArgumentException("Server URL path must end with /api/v1/.")
        }

        parsed.newBuilder()
            .encodedPath(normalizedPath)
            .build()
            .toString()
    }
}
