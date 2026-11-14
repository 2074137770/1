package com.example.keyremote.receiver

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuKeyInjector {

    private val isReady: Boolean
        get() = Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun inject(keyCode: Int) {
        if (!isReady) return
        try {
            Shizuku.newProcess(
                arrayOf("sh", "-c", "input keyevent $keyCode"),
                null,
                null
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        const val KEYCODE_DPAD_UP = 19
        const val KEYCODE_DPAD_DOWN = 20
        const val KEYCODE_DPAD_LEFT = 21
        const val KEYCODE_DPAD_RIGHT = 22
        const val KEYCODE_Z = 54
        const val KEYCODE_X = 52
        const val KEYCODE_C = 31
        const val KEYCODE_SPACE = 62
    }
}
