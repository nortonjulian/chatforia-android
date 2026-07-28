package com.chatforia.android.wireless

import android.content.Context

data class PendingWirelessCheckout(
    val userId: Int,
    val product: String,
    val checkoutAttemptId: String,
    val sessionId: String,
    val checkoutUrl: String
)

class WirelessCheckoutStore(
    context: Context
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun load(
        currentUserId: Int
    ): PendingWirelessCheckout? {
        require(currentUserId > 0) {
            "A valid current user ID is required."
        }

        val savedUserId =
            if (preferences.contains(KEY_USER_ID)) {
                preferences.getInt(KEY_USER_ID, -1)
            } else {
                null
            }

        /*
         * Earlier app versions did not save the owner of
         * the pending checkout. Never reuse an ownerless
         * checkout or one belonging to another account.
         */
        if (
            savedUserId == null ||
            savedUserId != currentUserId
        ) {
            clear()
            return null
        }

        val product =
            preferences.getString(KEY_PRODUCT, null)
                ?.takeIf { it.isNotBlank() }

        val checkoutAttemptId =
            preferences.getString(KEY_ATTEMPT_ID, null)
                ?.takeIf { it.isNotBlank() }

        val sessionId =
            preferences.getString(KEY_SESSION_ID, null)
                ?.takeIf { it.isNotBlank() }

        val checkoutUrl =
            preferences.getString(KEY_CHECKOUT_URL, null)
                ?.takeIf { it.isNotBlank() }

        if (
            product == null ||
            checkoutAttemptId == null ||
            sessionId == null ||
            checkoutUrl == null
        ) {
            clear()
            return null
        }

        return PendingWirelessCheckout(
            userId = savedUserId,
            product = product,
            checkoutAttemptId = checkoutAttemptId,
            sessionId = sessionId,
            checkoutUrl = checkoutUrl
        )
    }

    fun save(checkout: PendingWirelessCheckout) {
        require(checkout.userId > 0) {
            "A valid checkout owner ID is required."
        }

        preferences.edit()
            .putInt(KEY_USER_ID, checkout.userId)
            .putString(KEY_PRODUCT, checkout.product)
            .putString(
                KEY_ATTEMPT_ID,
                checkout.checkoutAttemptId
            )
            .putString(KEY_SESSION_ID, checkout.sessionId)
            .putString(
                KEY_CHECKOUT_URL,
                checkout.checkoutUrl
            )
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_PRODUCT)
            .remove(KEY_ATTEMPT_ID)
            .remove(KEY_SESSION_ID)
            .remove(KEY_CHECKOUT_URL)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME =
            "chatforia_wireless_checkout"

        const val KEY_USER_ID = "user_id"
        const val KEY_PRODUCT = "product"
        const val KEY_ATTEMPT_ID = "checkout_attempt_id"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_CHECKOUT_URL = "checkout_url"
    }
}
