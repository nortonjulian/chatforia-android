package com.chatforia.android.sounds

enum class RequiredPlan {
    Free,
    Premium
}

data class SoundOption(
    val label: String,
    val filename: String,
    val requiredPlan: RequiredPlan
)

object AppMessageTones {
    val all = listOf(
        SoundOption("Default", "Default.mp3", RequiredPlan.Free),
        SoundOption("Dreamer", "Dreamer.mp3", RequiredPlan.Premium),
        SoundOption("Happy Message", "Happy Message.mp3", RequiredPlan.Premium),
        SoundOption("Notify", "Notify.mp3", RequiredPlan.Premium),
        SoundOption("Pop", "Pop.mp3", RequiredPlan.Premium),
        SoundOption("Pulsating Sound", "Pulsating Sound.mp3", RequiredPlan.Premium),
        SoundOption("Sparkle", "Sparkle.mp3", RequiredPlan.Premium),
        SoundOption("Text Message", "Text Message.mp3", RequiredPlan.Premium),
        SoundOption("Vibrate", "Vibrate.mp3", RequiredPlan.Free),
        SoundOption("Xylophone", "Xylophone.mp3", RequiredPlan.Premium)
    )
}

object AppRingtones {
    val all = listOf(
        SoundOption("Bells", "Bells.mp3", RequiredPlan.Premium),
        SoundOption("Chimes", "Chimes.mp3", RequiredPlan.Premium),
        SoundOption("Classic", "Classic.mp3", RequiredPlan.Free),
        SoundOption("Digital Phone", "Digital Phone.mp3", RequiredPlan.Premium),
        SoundOption("Melodic", "Melodic.mp3", RequiredPlan.Premium),
        SoundOption("Organ Notes", "Organ Notes.mp3", RequiredPlan.Premium),
        SoundOption("Sound Reality", "Sound Reality.mp3", RequiredPlan.Premium),
        SoundOption("Street", "Street.mp3", RequiredPlan.Premium),
        SoundOption("Universfield", "Universfield.mp3", RequiredPlan.Premium),
        SoundOption("Urgency", "Urgency.mp3", RequiredPlan.Free)
    )
}