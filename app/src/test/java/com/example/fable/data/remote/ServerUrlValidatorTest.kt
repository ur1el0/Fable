package com.example.fable.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlValidatorTest {
    @Test
    fun normalizesBareHttpsHostToApiV1Slash() {
        val result = ServerUrlValidator.validateAndNormalize(
            rawUrl = "  https://fable-api-cbo8.onrender.com  ",
            allowCleartext = false
        )

        assertEquals("https://fable-api-cbo8.onrender.com/api/v1/", result.getOrNull())
    }

    @Test
    fun appendsTrailingSlashWhenEndingWithApiV1() {
        val result = ServerUrlValidator.validateAndNormalize(
            rawUrl = "https://fable-api-cbo8.onrender.com/api/v1",
            allowCleartext = false
        )

        assertEquals("https://fable-api-cbo8.onrender.com/api/v1/", result.getOrNull())
    }

    @Test
    fun allowsHttpCleartextOnlyWhenExplicitlyEnabled() {
        val allowed = ServerUrlValidator.validateAndNormalize(
            rawUrl = "http://10.0.2.2:8001/api/v1/",
            allowCleartext = true
        )
        val rejected = ServerUrlValidator.validateAndNormalize(
            rawUrl = "http://10.0.2.2:8001/api/v1/",
            allowCleartext = false
        )

        assertEquals("http://10.0.2.2:8001/api/v1/", allowed.getOrNull())
        assertTrue(rejected.isFailure)
        assertEquals("Only HTTPS server URLs are allowed.", rejected.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsBlankCredentialsQueryFragmentAndUnsupportedPaths() {
        val invalidInputs = listOf(
            "   ",
            "ftp://fable.test/api/v1/",
            "https://user:pass@fable.test/api/v1/",
            "https://fable.test/api/v1/?debug=1",
            "https://fable.test/api/v1/#section",
            "https://fable.test/api/v2/"
        )

        invalidInputs.forEach { rawUrl ->
            val result = ServerUrlValidator.validateAndNormalize(rawUrl, allowCleartext = true)
            assertTrue("Expected failure for $rawUrl", result.isFailure)
        }
    }
}
