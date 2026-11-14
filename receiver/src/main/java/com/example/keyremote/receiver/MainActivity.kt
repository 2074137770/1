package com.example.keyremote.receiver

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.net.ServerSocket
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 160, 48, 48)
            }

            status = TextView(this).apply {
                text = "正在检测环境..."
                textSize = 16f
            }

            startButton = Button(this).apply {
                text = "启动接收服务"
                setOnClickListener {
                    // 点击时再申请通知权限
                    if (Build.VERSION.SDK_INT >= 33) {
                        try {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
                        } catch (_: Exception) {}
                    }
                    startServiceSafely()
                }
            }

            layout.addView(status)
            layout.addView(startButton)
            setContentView(layout)

            // 延时 500 毫秒检测 Shizuku，避免阻塞主线程或系统接口崩溃
            status.postDelayed({ checkShizukuSafely() }, 500)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun checkShizukuSafely() {
        try {
            if (!Shizuku.pingBinder()) {
                status.text = "Shizuku 未运行，请先激活"
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                status.text = "Shizuku 已运行，请点击启动并授权"
            } else {
                status.text = "Shizuku 已就绪，可以启动"
            }
        } catch (e: Throwable) {
            status.text = "读取 Shizuku 状态失败，请尝试直接点击启动"
        }
    }

    private fun startServiceSafely() {
        try {
            if (!Shizuku.pingBinder()) {
                status.text = "启动失败：Shizuku 未运行"
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(0)
                status.text = "已弹出授权，请允许后再次点击启动"
                return
            }
            startForegroundService(Intent(this, KeyReceiverService::class.java))
            status.text = "服务已启动，端口 9999"
        } catch (e: Throwable) {
            status.text = "启动报错: ${e.message}"
        }
    }
}

class KeyReceiverService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel("k", "运行状态", NotificationManager.IMPORTANCE_LOW)
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
                val notification = Notification.Builder(this, "k")
                    .setContentTitle("按键接收端")
                    .setContentText("正在监听端口 9999")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build()
                startForeground(1, notification)
            }

            thread {
                try {
                    ServerSocket(9999).use { server ->
                        while (true) {
                            val client = server.accept()
                            thread {
                                try {
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
                                            try {
                                                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                                                method.isAccessible = true
                                                method.invoke(null, arrayOf("sh", "-c", "input keyevent $code"), null, null)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                } catch (_: Exception) {}
                                try { client.close() } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
