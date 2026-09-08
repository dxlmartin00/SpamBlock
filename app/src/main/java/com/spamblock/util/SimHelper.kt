package com.spamblock.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

data class SimCardInfo(
    val slotIndex: Int, // 0 for SIM 1, 1 for SIM 2
    val subId: Int,
    val displayName: String,
    val carrierName: String
)

object SimHelper {

    private const val TAG = "SimHelper"

    fun getActiveSims(context: Context): List<SimCardInfo> {
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        return try {
            val list = subManager.activeSubscriptionInfoList ?: emptyList<SubscriptionInfo>()
            list.map { sub ->
                val display = sub.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: "SIM ${sub.simSlotIndex + 1}"
                val carrier = sub.carrierName?.toString()?.takeIf { it.isNotBlank() }
                    ?: ""
                SimCardInfo(
                    slotIndex = sub.simSlotIndex,
                    subId = sub.subscriptionId,
                    displayName = display,
                    carrierName = carrier
                )
            }.sortedBy { it.slotIndex }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing READ_PHONE_STATE permission to inspect SIMs", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying active SIMs", e)
            emptyList()
        }
    }

    /**
     * Resolves physical SIM slot index (0 for SIM 1, 1 for SIM 2, -1 if unresolved)
     * using a multi-layer detection pipeline.
     */
    fun resolveSimSlot(context: Context, details: Call.Details): Int {
        val extras = details.extras
        val intentExtras = details.intentExtras

        // Layer 0: Check Call Details Extras for direct slot index or subscription index
        val slotFromExtras = resolveSlotFromBundle(extras) ?: resolveSlotFromBundle(intentExtras)
        if (slotFromExtras != null && slotFromExtras in 0..1) {
            Log.d(TAG, "Layer 0: Resolved SIM slot $slotFromExtras from call extras")
            return slotFromExtras
        }

        val accountHandle: PhoneAccountHandle? = details.accountHandle
            ?: extras?.let { androidx.core.os.BundleCompat.getParcelable(it, TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountHandle::class.java) }
            ?: intentExtras?.let { androidx.core.os.BundleCompat.getParcelable(it, TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountHandle::class.java) }

        val accountId = accountHandle?.id?.trim() ?: ""
        Log.d(TAG, "Resolving SIM slot for PhoneAccountHandle: handle=$accountHandle, id='$accountId'")

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

        // Layer 1: Official Android API 30+ TelephonyManager.getSubscriptionId(accountHandle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && accountHandle != null && telephonyManager != null) {
            try {
                val subId = telephonyManager.getSubscriptionId(accountHandle)
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val slot = SubscriptionManager.getSlotIndex(subId)
                    if (slot in 0..1) {
                        Log.d(TAG, "Layer 1: Resolved SIM slot $slot via TelephonyManager.getSubscriptionId($subId)")
                        return slot
                    }
                    if (subManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        val subInfo = subManager.getActiveSubscriptionInfo(subId)
                        if (subInfo != null && subInfo.simSlotIndex in 0..1) {
                            Log.d(TAG, "Layer 1: Resolved SIM slot ${subInfo.simSlotIndex} via getActiveSubscriptionInfo($subId)")
                            return subInfo.simSlotIndex
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Layer 1 check failed: ${e.message}")
            }
        }

        // Layer 2: Match against TelecomManager.getCallCapablePhoneAccounts()
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager != null && accountHandle != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val callAccounts = telecomManager.getCallCapablePhoneAccounts()
                for ((index, acc) in callAccounts.withIndex()) {
                    if (acc == accountHandle || acc.id == accountHandle.id) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && telephonyManager != null) {
                            val subId = telephonyManager.getSubscriptionId(acc)
                            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                                val slot = SubscriptionManager.getSlotIndex(subId)
                                if (slot in 0..1) {
                                    Log.d(TAG, "Layer 2: Resolved SIM slot $slot via call capable account subId")
                                    return slot
                                }
                            }
                        }
                        if (index in 0..1) {
                            Log.d(TAG, "Layer 2: Fallback slot $index from callCapablePhoneAccounts index")
                            return index
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Layer 2 check failed: ${e.message}")
            }
        }

        // Layer 3: Query active subscriptions (matches ICCID, subId, cardId)
        if (subManager != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val activeList = subManager.activeSubscriptionInfoList ?: emptyList()

                // If device only has 1 active SIM, all calls are on that SIM
                if (activeList.size == 1) {
                    val onlySlot = activeList[0].simSlotIndex
                    if (onlySlot in 0..1) {
                        Log.d(TAG, "Layer 3: Single active SIM detected. Slot $onlySlot")
                        return onlySlot
                    }
                }

                if (accountId.isNotEmpty()) {
                    for (sub in activeList) {
                        val subIdStr = sub.subscriptionId.toString()
                        val slotStr = sub.simSlotIndex.toString()

                        // Check subId match
                        if (accountId == subIdStr || accountId.contains("sub_$subIdStr") || accountId.contains("subId_$subIdStr")) {
                            Log.d(TAG, "Layer 3: Resolved SIM slot ${sub.simSlotIndex} via subId '$accountId'")
                            return sub.simSlotIndex
                        }

                        // Check direct slot index
                        if (accountId == slotStr) {
                            Log.d(TAG, "Layer 3: Resolved SIM slot ${sub.simSlotIndex} via direct slot string")
                            return sub.simSlotIndex
                        }

                        // Check ICCID match (standard on Realme / ColorOS / Samsung)
                        val iccId = try { sub.iccId } catch (_: Exception) { null }
                        if (!iccId.isNullOrBlank()) {
                            if (accountId.equals(iccId, ignoreCase = true) ||
                                accountId.contains(iccId, ignoreCase = true) ||
                                iccId.contains(accountId, ignoreCase = true)) {
                                Log.d(TAG, "Layer 3: Resolved SIM slot ${sub.simSlotIndex} via ICCID match '$accountId'")
                                return sub.simSlotIndex
                            }
                        }

                        // Check card ID match
                        val cardId = try { sub.cardId.toString() } catch (_: Exception) { null }
                        if (!cardId.isNullOrBlank() && (accountId == cardId || accountId.contains(cardId))) {
                            Log.d(TAG, "Layer 3: Resolved SIM slot ${sub.simSlotIndex} via cardId '$accountId'")
                            return sub.simSlotIndex
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Layer 3 check failed: ${e.message}")
            }
        }

        // Layer 4: Textual heuristic on accountId (e.g. "slot_0", "sim1", etc.)
        if (accountId.isNotEmpty()) {
            val heuristicSlot = parseSlotFromText(accountId)
            if (heuristicSlot in 0..1) {
                Log.d(TAG, "Layer 4: Resolved SIM slot $heuristicSlot via heuristic string '$accountId'")
                return heuristicSlot
            }
        }

        Log.w(TAG, "Unable to resolve SIM slot for incoming call. handle=$accountHandle, id='$accountId'")
        return -1
    }

    private fun resolveSlotFromBundle(bundle: Bundle?): Int? {
        if (bundle == null) return null

        val slotKeys = listOf(
            SubscriptionManager.EXTRA_SLOT_INDEX,
            "android.telephony.extra.SLOT_INDEX",
            "simSlot",
            "sim_slot",
            "slotIndex",
            "slot_index",
            "slot",
            "phone"
        )
        for (key in slotKeys) {
            if (bundle.containsKey(key)) {
                val value = bundle.getInt(key, -1)
                if (value in 0..1) return value
            }
        }

        val subKeys = listOf(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            "android.telephony.extra.SUBSCRIPTION_INDEX",
            "subscription",
            "subId",
            "sub_id"
        )
        for (key in subKeys) {
            if (bundle.containsKey(key)) {
                val subId = bundle.getInt(key, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val slot = SubscriptionManager.getSlotIndex(subId)
                    if (slot in 0..1) return slot
                }
            }
        }

        return null
    }

    /**
     * Parses slot index (0 for SIM 1, 1 for SIM 2) from arbitrary account id strings.
     */
    fun parseSlotFromText(text: String): Int {
        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return -1

        // Direct digit
        if (clean == "0") return 0
        if (clean == "1") return 1

        // Explicit slot / sim markers
        if (clean.contains("slot_0") || clean.contains("slot0") || clean.contains("slot:0") || clean.endsWith("_0")) return 0
        if (clean.contains("slot_1") || clean.contains("slot1") || clean.contains("slot:1") || clean.endsWith("_1")) return 1

        if (clean.contains("sim1") || clean.contains("sim_1") || clean.contains("sim-1") || clean.contains("sim:1")) return 0
        if (clean.contains("sim2") || clean.contains("sim_2") || clean.contains("sim-2") || clean.contains("sim:2")) return 1

        if (clean.contains("sub_0") || clean.contains("sub0")) return 0
        if (clean.contains("sub_1") || clean.contains("sub1")) return 1

        return -1
    }
}
