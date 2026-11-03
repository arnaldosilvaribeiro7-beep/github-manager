package com.example.fallinghello

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class FallingHelloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 84f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f
    private var gravity = 0.85f
    private var lastFrame = 0L
    private var started = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startFall()
    }

    fun startFall() {
        if (started) return
        started = true
        lastFrame = System.nanoTime()
        postOnAnimation { updatePhysics() }
    }

    private fun updatePhysics() {
        if (!started) return

        val now = System.nanoTime()
        val deltaSeconds = ((now - lastFrame) / 1_000_000_000.0).coerceAtLeast(0.016).toFloat()
        lastFrame = now

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (x == 0f && y == 0f) {
            x = viewWidth / 2f
            y = -textPaint.textSize
            vy = 12f
            vx = (Random.nextFloat() - 0.5f) * 16f
        }

        x += vx * deltaSeconds * 60f
        vy += gravity * deltaSeconds * 60f
        y += vy * deltaSeconds * 60f

        val floorY = viewHeight - 80f
        if (y >= floorY) {
            y = floorY
            vy *= -0.52f
            vx *= 0.96f

            if (abs(vy) < 2f) {
                vy = 0f
            }
        }

        if (x < 0f || x > viewWidth) {
            x = x.coerceIn(0f, viewWidth)
            vx *= -0.78f
        }

        invalidate()
        postOnAnimation { updatePhysics() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText("Hello World", x, y, textPaint)
    }
}
