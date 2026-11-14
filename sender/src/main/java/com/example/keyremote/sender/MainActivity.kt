package com.example.keyremote.sender

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KeyRemoteApp() }
    }
}

@Composable
fun KeyRemoteApp() {
    var ip by remember { mutableStateOf("192.168.1.100") }
    var status by remember { mutableStateOf("未连接") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "局域网按键映射",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A)),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = {
                        status = "连接中..."
                        TcpSender.connect(ip, 9999) { ok ->
                            status = if (ok) "已连接" else "连接失败"
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3482FF))
                ) {
                    Text("连接")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(status, color = Color(0xFF666666), fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))
            DPad(onCommand = { TcpSender.send(it) })

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RemoteButton("点赞\nZ", Modifier.weight(1f)) { TcpSender.send("Z") }
                Spacer(Modifier.width(12.dp))
                RemoteButton("评论\nX", Modifier.weight(1f)) { TcpSender.send("X") }
                Spacer(Modifier.width(12.dp))
                RemoteButton("收藏\nC", Modifier.weight(1f)) { TcpSender.send("C") }
            }
        }
    }
}

@Composable
private fun DPad(onCommand: (String) -> Unit) {
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        RemoteButton("↑", Modifier.align(Alignment.TopCenter).size(76.dp)) { onCommand("UP") }
        RemoteButton("↓", Modifier.align(Alignment.BottomCenter).size(76.dp)) { onCommand("DOWN") }
        RemoteButton("←", Modifier.align(Alignment.CenterStart).size(76.dp)) { onCommand("LEFT") }
        RemoteButton("→", Modifier.align(Alignment.CenterEnd).size(76.dp)) { onCommand("RIGHT") }
        RemoteButton("空格", Modifier.size(76.dp), primary = true) { onCommand("SPACE") }
    }
}

@Composable
private fun RemoteButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Color(0xFF3482FF) else Color.White,
            contentColor = if (primary) Color.White else Color(0xFF1A1A1A)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

object TcpSender {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val main = Handler(Looper.getMainLooper())

    fun connect(ip: String, port: Int, callback: (Boolean) -> Unit) {
        thread {
            try {
                socket?.close()
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 3000)
                socket = s
                writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                main.post { callback(true) }
            } catch (_: Exception) {
                main.post { callback(false) }
            }
        }
    }

    fun send(command: String) {
        thread {
            try {
                writer?.apply {
                    write(command)
                    newLine()
                    flush()
                }
            } catch (_: Exception) {
            }
        }
    }
}
