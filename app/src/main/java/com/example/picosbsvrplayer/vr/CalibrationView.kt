package com.example.picosbsvrplayer.vr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CalibrationView(context: Context) : View(context) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val half = width / 2f
    canvas.drawColor(Color.BLACK)
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(55, 8, 8)
    canvas.drawRect(0f, 0f, half, height.toFloat(), paint)
    paint.color = Color.rgb(8, 18, 55)
    canvas.drawRect(half, 0f, width.toFloat(), height.toFloat(), paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = Color.WHITE
    val step = (width / 16f).coerceAtLeast(40f)
    var x = 0f
    while (x <= width) {
      canvas.drawLine(x, 0f, x, height.toFloat(), paint)
      x += step
    }
    var y = 0f
    while (y <= height) {
      canvas.drawLine(0f, y, width.toFloat(), y, paint)
      y += step
    }
    canvas.drawLine(half, 0f, half, height.toFloat(), paint)

    paint.style = Paint.Style.FILL
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = (height / 9f).coerceAtMost(220f)
    paint.color = Color.WHITE
    canvas.drawText("L", half / 2f, height / 2f, paint)
    canvas.drawText("R", half + half / 2f, height / 2f, paint)
    paint.textSize = (height / 35f).coerceAtMost(64f)
    canvas.drawText("PICO SBS VR · ${width}×${height}", width / 2f, height * 0.92f, paint)
  }
}
