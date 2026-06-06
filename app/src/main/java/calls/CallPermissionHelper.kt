package com.chatforia.android.calls

import android.Manifest
import android.os.Build

object CallPermissionHelper {

    fun audioPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    fun videoPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }
}