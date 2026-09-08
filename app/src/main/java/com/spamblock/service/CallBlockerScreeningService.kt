package com.spamblock.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.spamblock.data.BlockedCallsDbHelper
import com.spamblock.data.PreferencesManager
import com.spamblock.util.ContactChecker
import com.spamblock.util.NotificationHelper

class CallBlockerScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "SpamBlockService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = PreferencesManager.getInstance(applicationContext)
        val db = BlockedCallsDbHelper.getInstance(applicationContext)

        prefs.incrementScreened()

        val handle = callDetails.handle
        val rawNumber = when {
            handle == null -> ""
            handle.scheme.equals("tel", ignoreCase = true) -> handle.schemeSpecificPart ?: ""
            else -> handle.schemeSpecificPart ?: handle.toString()
        }.trim()

        val isPrivate = rawNumber.isBlank() ||
                rawNumber.equals("private", ignoreCase = true) ||
                rawNumber.equals("unknown", ignoreCase = true) ||
                rawNumber.equals("restricted", ignoreCase = true) ||
                rawNumber.equals("anonymous", ignoreCase = true)

        val simSlot = com.spamblock.util.SimHelper.resolveSimSlot(applicationContext, callDetails)
        Log.d(TAG, "Incoming call screened: rawNumber='$rawNumber', isPrivate=$isPrivate, simSlot=$simSlot")

        // Per-SIM Card Slot Protection Rule Check
        if (simSlot == 0 && !prefs.sim1Protected) {
            Log.i(TAG, "SIM 1 protection is turned OFF. Allowing call: $rawNumber")
            allowCall(callDetails)
            return
        }
        if (simSlot == 1 && !prefs.sim2Protected) {
            Log.i(TAG, "SIM 2 protection is turned OFF. Allowing call: $rawNumber")
            allowCall(callDetails)
            return
        }

        if (isPrivate) {
            if (prefs.blockPrivateNumbers) {
                Log.i(TAG, "Blocking private/hidden call on SIM $simSlot")
                blockCall(callDetails, "Private / Hidden Caller", "Private Number", simSlot, prefs, db)
                return
            } else {
                allowCall(callDetails)
                return
            }
        }

        // Check Whitelist
        if (prefs.isWhitelisted(rawNumber)) {
            Log.i(TAG, "Call from whitelisted number: $rawNumber")
            allowCall(callDetails)
            return
        }

        // Check if Unknown (Not in Contacts)
        if (prefs.blockUnknownNumbers) {
            val isKnownContact = ContactChecker.isContact(this, rawNumber)
            if (!isKnownContact) {
                Log.i(TAG, "Blocking unknown caller (not in contacts): $rawNumber on SIM $simSlot")
                blockCall(callDetails, "Not in Contacts", rawNumber, simSlot, prefs, db)
                return
            }
        }

        // If known contact or unknown blocking disabled
        Log.i(TAG, "Allowing call from known contact: $rawNumber")
        allowCall(callDetails)
    }

    private fun blockCall(
        details: Call.Details,
        reason: String,
        displayNumber: String,
        simSlot: Int,
        prefs: PreferencesManager,
        db: BlockedCallsDbHelper
    ) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(prefs.rejectCall)
            .setSkipNotification(true)
            .setSkipCallLog(false)
            .build()

        respondToCall(details, response)

        // Asynchronously or quickly record to database with SIM slot
        db.insert(displayNumber, reason, simSlot)

        if (prefs.notifyOnBlocked) {
            val simLabel = when (simSlot) {
                0 -> "[SIM 1] "
                1 -> "[SIM 2] "
                else -> ""
            }
            NotificationHelper.notifyBlockedCall(this, "$simLabel$displayNumber", reason)
        }
    }

    private fun allowCall(details: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipNotification(false)
            .build()

        respondToCall(details, response)
    }
}
