package com.iwedia.cltv.platform.model.fast_backend_utils

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import com.iwedia.cltv.platform.model.Constants
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


object UserProfileHelper {
    const val TAG = "UserProfileHelper"
    private var userHashedAccounts: String = ""

    fun getGmailAccounts(context: Context): List<String> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.GET_ACCOUNTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Read contacts permission not granted, can read email addresses")
            return emptyList()
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Permissions granted. Get accounts")
            val accounts = AccountManager.get(context).accounts
            val hashedAccounts = mutableListOf<String>()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getGmailAccounts ${accounts.size}")
            for (account in accounts) {
                val email = account.name
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Gmail account: $email")
                val hashedEmail = hashAccount(email)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Hashed Gmail account: $hashedEmail")
                hashedAccounts.add(hashedEmail)
                if (hashedAccounts.size > 0) break

            }
            //userHashedAccounts = concatenateHashedEmails(hashedAccounts)
            if (hashedAccounts.isNotEmpty()) {
                userHashedAccounts = hashedAccounts[0]
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Concatenated hashed Gmail account: $userHashedAccounts")
            return hashedAccounts
        }
    }

    private fun hashAccount(account: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = messageDigest.digest(account.toByteArray(StandardCharsets.UTF_8))

        val stringBuffer = StringBuilder()
        for (i in hashedBytes.indices) {
            val unsignedByte = hashedBytes[i].toInt() and 0xff
            stringBuffer.append(Integer.toString(unsignedByte + 0x100, 16).substring(1))
        }
        return stringBuffer.toString()
    }

    fun getUserAccountsHashed(): String {
        return userHashedAccounts
    }

    private fun concatenateHashedEmails(emails: List<String>): String {
        return if (!emails.isNullOrEmpty())
            emails.joinToString(",")
        else ""
    }
}