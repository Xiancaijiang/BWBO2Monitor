package com.example.bwbo2monitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val wavePoints = mutableListOf<Float>()

    init {
        // 随机生成波形数据
        generateWaveform()
    }

    private fun generateWaveform() {
        val width = width
        val height = height
        for (i in 0 until width step 5) {
            val y = height / 2 + (Math.random() * 100 - 50).toFloat()
            wavePoints.add(i.toFloat())
            wavePoints.add(y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until wavePoints.size - 2 step 2) {
            canvas.drawLine(wavePoints[i], wavePoints[i + 1], wavePoints[i + 2], wavePoints[i + 3], paint)
        }
    }
}