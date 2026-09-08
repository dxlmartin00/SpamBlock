package com.spamblock.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "spamblock_prefs"
        private const val KEY_BLOCK_UNKNOWN = "block_unknown"
        private const val KEY_BLOCK_PRIVATE = "block_private"
        private const val KEY_REJECT_CALL = "reject_call"
        private const val KEY_NOTIFY_ON_BLOCKED = "notify_on_blocked"
        private const val KEY_WHITELIST = "whitelist_numbers"
        private const val KEY_TOTAL_SCREENED = "total_screened_calls"
        private const val KEY_SIM1_PROTECTED = "sim1_protected"
        private const val KEY_SIM2_PROTECTED = "sim2_protected"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _whitelistFlow = MutableStateFlow<Set<String>>(emptySet())
    val whitelistFlow: StateFlow<Set<String>> = _whitelistFlow.asStateFlow()

    init {
        _whitelistFlow.value = getWhitelist()
    }

    var blockUnknownNumbers: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_UNKNOWN, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_UNKNOWN, value).apply()

    var blockPrivateNumbers: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_PRIVATE, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_PRIVATE, value).apply()

    var rejectCall: Boolean
        get() = prefs.getBoolean(KEY_REJECT_CALL, true)
        set(value) = prefs.edit().putBoolean(KEY_REJECT_CALL, value).apply()

    var notifyOnBlocked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_BLOCKED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_BLOCKED, value).apply()

    var sim1Protected: Boolean
        get() = prefs.getBoolean(KEY_SIM1_PROTECTED, true)
        set(value) = prefs.edit().putBoolean(KEY_SIM1_PROTECTED, value).apply()

    var sim2Protected: Boolean
        get() = prefs.getBoolean(KEY_SIM2_PROTECTED, true)
        set(value) = prefs.edit().putBoolean(KEY_SIM2_PROTECTED, value).apply()

    fun isSimSlotProtected(slot: Int): Boolean {
        return when (slot) {
            0 -> sim1Protected
            1 -> sim2Protected
            else -> true
        }
    }

    var totalScreened: Int
        get() = prefs.getInt(KEY_TOTAL_SCREENED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_SCREENED, value).apply()

    fun incrementScreened() {
        totalScreened = totalScreened + 1
    }

    fun getWhitelist(): Set<String> {
        return prefs.getStringSet(KEY_WHITELIST, emptySet())?.toSet() ?: emptySet()
    }

    fun addWhitelist(number: String) {
        val normalized = normalizeNumber(number)
        val current = getWhitelist().toMutableSet()
        current.add(normalized)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
        _whitelistFlow.value = current
    }

    fun removeWhitelist(number: String) {
        val normalized = normalizeNumber(number)
        val current = getWhitelist().toMutableSet()
        current.remove(normalized)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
        _whitelistFlow.value = current
    }

    fun isWhitelisted(number: String): Boolean {
        val normalized = normalizeNumber(number)
        val whitelist = getWhitelist()
        return whitelist.contains(normalized) || whitelist.any {
            normalized.endsWith(it) || it.endsWith(normalized)
        }
    }

    fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }
}
