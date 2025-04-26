package com.example.bwbo2monitor

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 欢迎界面，进入app延时三秒。
 *
 * @author Xiancaijiang
 * @Time 2025-4-25
 */

class WelcomeActivity : AppCompatActivity() {
    private lateinit var tvCountdown: TextView
    private lateinit var countDownTimer: CountDownTimer
    private val timeLeftInMillis: Long = 3000  // 设置三秒进入主界面


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcome)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化倒数计时栏
        tvCountdown = findViewById(R.id.tv_countdown)

        // 调用计时函数
        startCountdown();
    }

    //计时函数
    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the countdown TextView if needed
                val secondsLeft = (millisUntilFinished / 1000).toInt();
                tvCountdown.text = secondsLeft.toString();
            }
            //计时结束，跳转
            override fun onFinish() {
                startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                finish();
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保倒计时器被取消，以防止内存泄漏
        if (::countDownTimer.isInitialized) { // 检查 countDownTimer 是否已初始化
            countDownTimer.cancel() // 取消计时器
        }
    }
}
