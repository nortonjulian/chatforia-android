package com.chatforia.android.privacy

/** Only the signed-in account's persisted setting may control task previews. */
internal fun shouldProtectRecents(
    signedIn: Boolean,
    blurWhenUnfocused: Boolean
): Boolean = signedIn && blurWhenUnfocused
