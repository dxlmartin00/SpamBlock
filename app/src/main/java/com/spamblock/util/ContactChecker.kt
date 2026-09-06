package com.spamblock.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactChecker {
    private const val TAG = "ContactChecker"

    fun isContact(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                it.count > 0
            } ?: false
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts permission not granted: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contact: ${e.message}", e)
            false
        }
    }
}
