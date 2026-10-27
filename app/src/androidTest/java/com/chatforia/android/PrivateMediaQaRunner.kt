package com.chatforia.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Opt-in emulator-only private-media QA. Does not start the normal Chatforia
 * application and its analytics, FCM or background badge reconciliation.
 * Enabled only with -PCHATFORIA_PRIVATE_MEDIA_QA=true.
 */
class PrivateMediaQaRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
