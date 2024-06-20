package com.objectdetectiontfdemo.tfhelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.objectdetectiontfdemo.R
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.color_a)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            changeBoxColor(result.categories[0].label)

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    private fun changeBoxColor(label: String?) {
        val firstChar = label?.firstOrNull()

        val boxColor = when (firstChar?.uppercaseChar()) {
            'A' -> ContextCompat.getColor(context!!, R.color.color_a)
            'B' -> ContextCompat.getColor(context!!, R.color.color_b)
            'C' -> ContextCompat.getColor(context!!, R.color.color_c)
            'D' -> ContextCompat.getColor(context!!, R.color.color_d)
            'E' -> ContextCompat.getColor(context!!, R.color.color_e)
            'F' -> ContextCompat.getColor(context!!, R.color.color_f)
            'G' -> ContextCompat.getColor(context!!, R.color.color_g)
            'H' -> ContextCompat.getColor(context!!, R.color.color_h)
            'I' -> ContextCompat.getColor(context!!, R.color.color_i)
            'J' -> ContextCompat.getColor(context!!, R.color.color_j)
            'K' -> ContextCompat.getColor(context!!, R.color.color_k)
            'L' -> ContextCompat.getColor(context!!, R.color.color_l)
            'M' -> ContextCompat.getColor(context!!, R.color.color_m)
            'N' -> ContextCompat.getColor(context!!, R.color.color_n)
            'O' -> ContextCompat.getColor(context!!, R.color.color_o)
            'P' -> ContextCompat.getColor(context!!, R.color.color_p)
            'Q' -> ContextCompat.getColor(context!!, R.color.color_q)
            'R' -> ContextCompat.getColor(context!!, R.color.color_r)
            'S' -> ContextCompat.getColor(context!!, R.color.color_s)
            'T' -> ContextCompat.getColor(context!!, R.color.color_t)
            'U' -> ContextCompat.getColor(context!!, R.color.color_u)
            'V' -> ContextCompat.getColor(context!!, R.color.color_v)
            'W' -> ContextCompat.getColor(context!!, R.color.color_w)
            'X' -> ContextCompat.getColor(context!!, R.color.color_x)
            'Y' -> ContextCompat.getColor(context!!, R.color.color_y)
            'Z' -> ContextCompat.getColor(context!!, R.color.color_z)
            else -> ContextCompat.getColor(context!!, R.color.color_a)
        }

        boxPaint.color = boxColor
    }

    fun setResults(
        detectionResults: MutableList<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
