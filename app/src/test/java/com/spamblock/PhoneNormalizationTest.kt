package com.spamblock

import org.junit.Assert.*
import org.junit.Test

class PhoneNormalizationTest {

    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    private fun isWhitelisted(candidate: String, whitelist: Set<String>): Boolean {
        val normalized = normalizeNumber(candidate)
        return whitelist.contains(normalized) || whitelist.any {
            normalized.endsWith(it) || it.endsWith(normalized)
        }
    }

    @Test
    fun testNormalizationRemovesDashesAndSpaces() {
        val raw = "+1 (555) 123-4567"
        val normalized = normalizeNumber(raw)
        assertEquals("+15551234567", normalized)
    }

    @Test
    fun testWhitelistMatching() {
        val whitelist = setOf("+15551234567", "911", "18005550199")
        assertTrue(isWhitelisted("5551234567", whitelist))
        assertTrue(isWhitelisted("+1 (555) 123-4567", whitelist))
        assertTrue(isWhitelisted("911", whitelist))
        assertFalse(isWhitelisted("+19998887777", whitelist))
    }

    @Test
    fun testPrivateCallerDetection() {
        val testCases = listOf("", "private", "UNKNOWN", "Restricted", "anonymous")
        for (case in testCases) {
            val isPrivate = case.isBlank() ||
                    case.equals("private", ignoreCase = true) ||
                    case.equals("unknown", ignoreCase = true) ||
                    case.equals("restricted", ignoreCase = true) ||
                    case.equals("anonymous", ignoreCase = true)
            assertTrue("Expected '$case' to be treated as private/hidden", isPrivate)
        }
    }
}
