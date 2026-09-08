package com.spamblock.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
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

    fun resolveSimSlot(context: Context, details: Call.Details): Int {
        val accountHandle = details.accountHandle ?: return -1
        val accountId = accountHandle.id?.trim() ?: return -1

        Log.d(TAG, "Resolving SIM slot for PhoneAccountHandle id='$accountId'")

        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return -1

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            if (accountId == "0") return 0
            if (accountId == "1") return 1
            return -1
        }

        return try {
            val activeList = subManager.activeSubscriptionInfoList ?: return -1
            for (sub in activeList) {
                val subIdStr = sub.subscriptionId.toString()
                val slotStr = sub.simSlotIndex.toString()

                if (accountId == subIdStr || accountId == slotStr) {
                    return sub.simSlotIndex
                }

                if (accountId.contains(subIdStr)) {
                    return sub.simSlotIndex
                }
            }
            -1
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving SIM slot: ${e.message}")
            -1
        }
    }
}
