package com.chatforia.android.sounds

import com.chatforia.android.R

enum class RequiredPlan {
    Free,
    Premium
}

data class SoundOption(
    val labelResId: Int,
    val filename: String,
    val requiredPlan: RequiredPlan
)

object AppMessageTones {
    val all = listOf(
        SoundOption(R.string.android_sound_default, "Default.mp3", RequiredPlan.Free),
        SoundOption(R.string.android_sound_dreamer, "dreamer.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_happy_message, "Happy Message.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_notify, "notify.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_pop, "pop.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_pulsating_sound, "Pulsating Sound.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_sparkle, "sparkle.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_text_message, "Text Message.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_vibrate, "vibrate.mp3", RequiredPlan.Free),
        SoundOption(R.string.android_sound_xylophone, "xylophone.mp3", RequiredPlan.Premium)
    )
}

object AppRingtones {
    val all = listOf(
        SoundOption(R.string.android_sound_bells, "bells.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_chimes, "chimes.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_classic, "classic.mp3", RequiredPlan.Free),
        SoundOption(R.string.android_sound_digital_phone, "Digital Phone.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_melodic, "melodic.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_organ_notes, "Organ Notes.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_sound_reality, "Sound Reality.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_street, "street.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_universfield, "universfield.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_urgency, "urgency.mp3", RequiredPlan.Free)
    )
}

fun SoundOption.isAvailableForPlan(plan: String?): Boolean {
    val normalizedPlan = plan?.uppercase() ?: "FREE"

    return requiredPlan == RequiredPlan.Free ||
            normalizedPlan in listOf("PLUS", "PREMIUM", "WIRELESS")
}

fun resolvedMessageToneForPlan(
    filename: String?,
    plan: String?
): String {
    val fallback = "Default.mp3"

    val option =
        AppMessageTones.all.firstOrNull {
            it.filename.equals(filename, ignoreCase = true)
        }

    return if (option?.isAvailableForPlan(plan) == true) {
        option.filename
    } else {
        fallback
    }
}

fun resolvedRingtoneForPlan(
    filename: String?,
    plan: String?
): String {
    val fallback = "classic.mp3"

    val option =
        AppRingtones.all.firstOrNull {
            it.filename.equals(filename, ignoreCase = true)
        }

    return if (option?.isAvailableForPlan(plan) == true) {
        option.filename
    } else {
        fallback
    }
}