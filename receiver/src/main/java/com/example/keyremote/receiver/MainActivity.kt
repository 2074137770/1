package com.example.keyremote.receiver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 160, 48, 48)
        }

        val title = TextView(this).apply {
            text = "按键映射接收端"
            textSize = 24f
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Shizuku 状态未知"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val start = Button(this).apply {
            text = "启动接收服务"
            setOnClickListener { ensureShizukuAndStart() }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(start)
        setContentView(layout)

        updateShizukuStatus()
    }

    private fun updateShizukuStatus() {
        status.text = when {
            !Shizuku.pingBinder() -> "Shizuku 未运行，请先激活 Shizuku"
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Shizuku 已授权"
            else -> "Shizuku 已运行，但未授权"
        }
    }

    private fun ensureShizukuAndStart() {
        if (!Shizuku.pingBinder()) {
            status.text = "Shizuku 未运行"
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
            status.text = "已请求授权，请允许后再次点击"
            return
        }

        startForegroundService(Intent(this, KeyReceiverService::class.java))
        status.text = "服务已启动，端口 9999"
    }
}
