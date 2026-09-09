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

    @Test
    fun testSimSlotProtectionPolicy() {
        // Scenario 1: SIM 1 Protected, SIM 2 Open
        val sim1Protected = true
        val sim2Protected = false

        fun shouldScreenCall(slot: Int): Boolean {
            return when (slot) {
                0 -> sim1Protected
                1 -> sim2Protected
                else -> true
            }
        }

        assertTrue("Calls on SIM 1 should be screened", shouldScreenCall(0))
        assertFalse("Calls on SIM 2 should bypass screening", shouldScreenCall(1))
        assertTrue("Unknown slot should be screened by default", shouldScreenCall(-1))

        // Scenario 2: Both Open
        val bothOpenPolicy = fun(slot: Int): Boolean {
            val sim1 = false
            val sim2 = false
            return when (slot) {
                0 -> sim1
                1 -> sim2
                else -> {
                    if (!sim1 && !sim2) false
                    else true
                }
            }
        }
        assertFalse("When both SIMs open, unknown slot should not be screened", bothOpenPolicy(-1))
        assertFalse("When both SIMs open, SIM 1 should not be screened", bothOpenPolicy(0))
        assertFalse("When both SIMs open, SIM 2 should not be screened", bothOpenPolicy(1))
    }

    @Test
    fun testSimSlotTextParsing() {
        assertEquals(0, com.spamblock.util.SimHelper.parseSlotFromText("0"))
        assertEquals(1, com.spamblock.util.SimHelper.parseSlotFromText("1"))
        assertEquals(0, com.spamblock.util.SimHelper.parseSlotFromText("slot_0"))
        assertEquals(1, com.spamblock.util.SimHelper.parseSlotFromText("slot_1"))
        assertEquals(0, com.spamblock.util.SimHelper.parseSlotFromText("telephony_slot0"))
        assertEquals(1, com.spamblock.util.SimHelper.parseSlotFromText("telephony_slot1"))
        assertEquals(0, com.spamblock.util.SimHelper.parseSlotFromText("sim1"))
        assertEquals(1, com.spamblock.util.SimHelper.parseSlotFromText("sim2"))
        assertEquals(0, com.spamblock.util.SimHelper.parseSlotFromText("sub_0"))
        assertEquals(1, com.spamblock.util.SimHelper.parseSlotFromText("sub_1"))
        assertEquals(-1, com.spamblock.util.SimHelper.parseSlotFromText("unknown_account"))
        assertEquals(-1, com.spamblock.util.SimHelper.parseSlotFromText(""))
    }

    @Test
    fun testUpdateCheckerVersionComparison() {
        // Newer versions
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("v2.1.0", "2.0.0"))
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("v2.0.1", "2.0.0"))
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("3.0.0", "2.0.0"))
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("v2.1", "2.0.0"))
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("2.0.1", "2.0"))

        // Same or older versions
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.0.0", "2.0.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("2.0.0", "2.0.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v1.9.9", "2.0.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.0", "2.0.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.0.0", "2.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v1.0.0", "2.0.0"))

        // Edge / empty cases
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("", "2.0.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.0.0", ""))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("invalid", "2.0.0"))

        // Version 2.2.0 comparisons
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("v2.3.0", "2.2.0"))
        assertTrue(com.spamblock.util.UpdateChecker.isNewerVersion("v2.2.1", "2.2.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.2.0", "2.2.0"))
        assertFalse(com.spamblock.util.UpdateChecker.isNewerVersion("v2.1.0", "2.2.0"))
    }

    @Test
    fun testRepeatedCallerPolicy() {
        fun shouldAllowRepeated(recentCount: Int, threshold: Int, allowRepeated: Boolean): Boolean {
            if (!allowRepeated) return false
            return recentCount >= (threshold - 1)
        }

        // Feature disabled
        assertFalse(shouldAllowRepeated(recentCount = 5, threshold = 2, allowRepeated = false))

        // Threshold = 2 (2nd call rings)
        assertFalse(shouldAllowRepeated(recentCount = 0, threshold = 2, allowRepeated = true))
        assertTrue(shouldAllowRepeated(recentCount = 1, threshold = 2, allowRepeated = true))
        assertTrue(shouldAllowRepeated(recentCount = 2, threshold = 2, allowRepeated = true))

        // Threshold = 3 (3rd call rings)
        assertFalse(shouldAllowRepeated(recentCount = 0, threshold = 3, allowRepeated = true))
        assertFalse(shouldAllowRepeated(recentCount = 1, threshold = 3, allowRepeated = true))
        assertTrue(shouldAllowRepeated(recentCount = 2, threshold = 3, allowRepeated = true))
        assertTrue(shouldAllowRepeated(recentCount = 3, threshold = 3, allowRepeated = true))
    }

    @Test
    fun testDialableNumberDetection() {
        assertTrue(com.spamblock.util.NotificationHelper.isDialableNumber("+15551234567"))
        assertTrue(com.spamblock.util.NotificationHelper.isDialableNumber("09123456789"))
        assertTrue(com.spamblock.util.NotificationHelper.isDialableNumber("911"))
        assertTrue(com.spamblock.util.NotificationHelper.isDialableNumber("(555) 123-4567"))

        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber(""))
        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber("private"))
        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber("UNKNOWN"))
        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber("restricted"))
        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber("anonymous"))
        assertFalse(com.spamblock.util.NotificationHelper.isDialableNumber("12"))
    }
}

