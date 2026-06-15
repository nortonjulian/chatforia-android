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
        SoundOption(R.string.android_sound_dreamer, "Dreamer.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_happy_message, "Happy Message.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_notify, "Notify.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_pop, "Pop.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_pulsating_sound, "Pulsating Sound.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_sparkle, "Sparkle.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_text_message, "Text Message.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_vibrate, "Vibrate.mp3", RequiredPlan.Free),
        SoundOption(R.string.android_sound_xylophone, "Xylophone.mp3", RequiredPlan.Premium)
    )
}

object AppRingtones {
    val all = listOf(
        SoundOption(R.string.android_sound_bells, "Bells.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_chimes, "Chimes.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_classic, "Classic.mp3", RequiredPlan.Free),
        SoundOption(R.string.android_sound_digital_phone, "Digital Phone.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_melodic, "Melodic.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_organ_notes, "Organ Notes.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_sound_reality, "Sound Reality.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_street, "Street.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_universfield, "Universfield.mp3", RequiredPlan.Premium),
        SoundOption(R.string.android_sound_urgency, "Urgency.mp3", RequiredPlan.Free)
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
    val fallback = "Classic.mp3"

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