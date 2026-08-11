package com.chatforia.android.notifications

enum class PushReconciliationDecision {
    COMPLETE,
    RETRY,
    USER_ACTION_REQUIRED
}

fun classifyPushReconciliation(
    result: PushRegistrationResult
): PushReconciliationDecision {
    return when (result) {
        is PushRegistrationResult.Success -> {
            if (result.twilioVoiceRegistered) {
                PushReconciliationDecision.COMPLETE
            } else {
                PushReconciliationDecision.RETRY
            }
        }

        is PushRegistrationResult.ReplacementRequired ->
            PushReconciliationDecision.USER_ACTION_REQUIRED

        is PushRegistrationResult.Failed ->
            PushReconciliationDecision.RETRY
    }
}
