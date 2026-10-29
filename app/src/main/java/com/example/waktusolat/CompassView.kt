package com.example.waktusolat

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var qiblaBearing: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var deviceAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var isAligned: Boolean = false
    var arrowColor: Int = Color.parseColor("#00A86B")
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.85f
        if (radius <= 0f) return

        if (qiblaBearing > 0f) {
            try {
                val rawAngle = ((qiblaBearing - deviceAzimuth) % 360f + 360f) % 360f
                if (!rawAngle.isNaN() && !rawAngle.isInfinite()) {
                    val needleAngle = Math.toRadians(rawAngle.toDouble())
                    
                    // Diamond needle dimensions
                    val headLen = radius * 0.60f
                    val tailLen = radius * 0.50f
                    val halfWidth = radius * 0.075f
                    
                    // Calculate key points
                    val tipX = cx + headLen * sin(needleAngle).toFloat()
                    val tipY = cy - headLen * cos(needleAngle).toFloat()
                    val tailX = cx - tailLen * sin(needleAngle).toFloat()
                    val tailY = cy + tailLen * cos(needleAngle).toFloat()
                    
                    // Perpendicular offset for width
                    val perpX = halfWidth * cos(needleAngle).toFloat()
                    val perpY = halfWidth * sin(needleAngle).toFloat()
                    
                    // Diamond path (4 points)
                    val needlePath = Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(cx + perpX, cy + perpY)
                        lineTo(tailX, tailY)
                        lineTo(cx - perpX, cy - perpY)
                        close()
                    }
                    
                    // Shadow for depth
                    val shadowOffset = radius * 0.02f
                    canvas.save()
                    canvas.translate(shadowOffset, shadowOffset)
                    canvas.drawPath(needlePath, Paint().apply {
                        style = Paint.Style.FILL
                        color = Color.argb(40, 0, 0, 0)
                    })
                    canvas.restore()
                    
                    // Main needle (theme color)
                    canvas.drawPath(needlePath, Paint().apply {
                        style = Paint.Style.FILL
                        color = arrowColor
                    })
                    
                    // Center pivot circle
                    canvas.drawCircle(cx, cy, radius * 0.04f, Paint().apply {
                        style = Paint.Style.FILL
                        color = Color.WHITE
                    })
                    canvas.drawCircle(cx, cy, radius * 0.04f, Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = radius * 0.01f
                        color = arrowColor
                    })
                    
                    // Alignment check
                    val diff = ((qiblaBearing - deviceAzimuth) % 360f + 360f) % 360f
                    val minDiff = if (diff > 180f) 360f - diff else diff
                    isAligned = minDiff < 3f
                } else {
                    isAligned = false
                }
            } catch (_: Exception) {
                isAligned = false
            }
        } else {
            isAligned = false
        }
    }
}
