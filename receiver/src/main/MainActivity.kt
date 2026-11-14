package com.example.keyremote.receiver

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.net.ServerSocket
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 48)
        }
        status = TextView(this).apply { text = "Shizuku 状态未知"; textSize = 16f }
        val start = Button(this).apply {
            text = "启动接收服务"
            setOnClickListener {
                if (!Shizuku.pingBinder()) {
                    status.text = "Shizuku 未运行"
                    return@setOnClickListener
                }
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(0)
                    status.text = "请允许授权后再点"
                    return@setOnClickListener
                }
                startForegroundService(Intent(this@MainActivity, KeyReceiverService::class.java))
                status.text = "服务已启动，端口 9999"
            }
        }
        layout.addView(status)
        layout.addView(start)
        setContentView(layout)
    }
}

class KeyReceiverService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel("k", "N", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(1, Notification.Builder(this, "k").setContentTitle("运行中").build())

        thread {
            try {
                ServerSocket(9999).use { server ->
                    while (true) {
                        val client = server.accept()
                        thread {
                            client.getInputStream().bufferedReader().forEachLine { line ->
                                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return@forEachLine
                                val code = when(line.trim()) {
                                    "UP" -> 19
                                    "DOWN" -> 20
                                    "LEFT" -> 21
                                    "RIGHT" -> 22
                                    "Z" -> 54
                                    "X" -> 52
                                    "C" -> 31
                                    "SPACE" -> 62
                                    else -> -1
                                }
                                if (code != -1) {
                                    Shizuku.newProcess(arrayOf("sh", "-c", "input keyevent $code"), null, null)
                                }
                            }
                            client.close()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
