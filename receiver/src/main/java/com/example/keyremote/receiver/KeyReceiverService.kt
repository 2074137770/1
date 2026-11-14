package com.example.keyremote.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.net.ServerSocket
import kotlin.concurrent.thread

class KeyReceiverService : Service() {

    private val injector = ShizukuKeyInjector()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())

        thread {
            try {
                ServerSocket(9999).use { server ->
                    while (true) {
                        val client = server.accept()
                        thread {
                            try {
                                client.getInputStream().bufferedReader().forEachLine { line ->
                                    val code = when (line.trim()) {
                                        "UP" -> ShizukuKeyInjector.KEYCODE_DPAD_UP
                                        "DOWN" -> ShizukuKeyInjector.KEYCODE_DPAD_DOWN
                                        "LEFT" -> ShizukuKeyInjector.KEYCODE_DPAD_LEFT
                                        "RIGHT" -> ShizukuKeyInjector.KEYCODE_DPAD_RIGHT
                                        "Z" -> ShizukuKeyInjector.KEYCODE_Z
                                        "X" -> ShizukuKeyInjector.KEYCODE_X
                                        "C" -> ShizukuKeyInjector.KEYCODE_C
                                        "SPACE" -> ShizukuKeyInjector.KEYCODE_SPACE
                                        else -> null
                                    }
                                    code?.let { injector.inject(it) }
                                }
                            } catch (_: Exception) {
                            } finally {
                                client.close()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "keyremote",
                "按键映射接收服务",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (android.os.Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "keyremote")
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("按键映射接收端")
            .setContentText("正在监听端口 9999")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .build()
    }
}
