package com.example.voicelock.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R

/**
 * Plain custom View (not a Fragment/ViewModel/Adapter/Dialog, so it is outside
 * CLAUDE.md's mandatory base-class table) rendering a 3x3 Android-style pattern
 * lock grid on Canvas and reporting the drawn node sequence via [PatternListener].
 */
class PatternLockView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {

        interface PatternListener {
            fun onPatternComplete(cells: List<Int>)

            fun onPatternTooShort()
        }

        private var patternListener: PatternListener? = null

        private val nodeCenters = Array(NODE_COUNT) { PointF() }
        private val selectedCells = mutableListOf<Int>()
        private var currentTouchX = 0f
        private var currentTouchY = 0f
        private var isDragging = false
        private var isErrorState = false
        private var isInputEnabled = true
        private var nodeRadius = 0f

        private val normalColor = ContextCompat.getColor(context, R.color.text_muted)
        private val selectedColor = ContextCompat.getColor(context, R.color.primary_blue)
        private val errorColor = ContextCompat.getColor(context, R.color.error_red)

        private val nodePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
        private val nodeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val linePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                strokeCap = Paint.Cap.ROUND
            }

        fun setListener(listener: PatternListener?) {
            patternListener = listener
        }

        fun setInputEnabled(enabled: Boolean) {
            isInputEnabled = enabled
            if (!enabled) resetDrawingState()
        }

        fun clear() {
            resetDrawingState()
        }

        fun showError() {
            isErrorState = true
            invalidate()
            postDelayed({ clear() }, ERROR_FLASH_DURATION_MS)
        }

        private fun resetDrawingState() {
            selectedCells.clear()
            isDragging = false
            isErrorState = false
            invalidate()
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            val size = minOf(w, h).toFloat()
            val cellSize = size / 3f
            nodeRadius = cellSize * 0.18f
            val offsetX = (w - size) / 2f
            val offsetY = (h - size) / 2f
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    nodeCenters[index] =
                        PointF(
                            offsetX + cellSize * col + cellSize / 2f,
                            offsetY + cellSize * row + cellSize / 2f,
                        )
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isInputEnabled) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resetDrawingState()
                    isDragging = true
                    handleTouch(event.x, event.y)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) handleTouch(event.x, event.y)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (event.action == MotionEvent.ACTION_UP) performClick()
                    isDragging = false
                    if (selectedCells.size in 1 until MIN_NODES) {
                        patternListener?.onPatternTooShort()
                    } else if (selectedCells.size >= MIN_NODES) {
                        patternListener?.onPatternComplete(selectedCells.toList())
                    }
                    invalidate()
                }
            }
            return true
        }

        override fun performClick(): Boolean = super.performClick()

        private fun handleTouch(
            x: Float,
            y: Float,
        ) {
            currentTouchX = x
            currentTouchY = y
            val hitIndex = hitTest(x, y)
            if (hitIndex != -1 && hitIndex !in selectedCells) {
                selectedCells.add(hitIndex)
            }
            invalidate()
        }

        private fun hitTest(
            x: Float,
            y: Float,
        ): Int {
            val slop = nodeRadius * 1.6f
            nodeCenters.forEachIndexed { index, center ->
                val dx = x - center.x
                val dy = y - center.y
                if (dx * dx + dy * dy <= slop * slop) return index
            }
            return -1
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val activeColor = if (isErrorState) errorColor else selectedColor

            if (selectedCells.size > 1) {
                linePaint.color = activeColor
                for (i in 0 until selectedCells.size - 1) {
                    val from = nodeCenters[selectedCells[i]]
                    val to = nodeCenters[selectedCells[i + 1]]
                    canvas.drawLine(from.x, from.y, to.x, to.y, linePaint)
                }
            }
            if (isDragging && selectedCells.isNotEmpty() && !isErrorState) {
                linePaint.color = activeColor
                val last = nodeCenters[selectedCells.last()]
                canvas.drawLine(last.x, last.y, currentTouchX, currentTouchY, linePaint)
            }

            nodeCenters.forEachIndexed { index, center ->
                val isSelected = index in selectedCells
                nodePaint.color = if (isSelected) activeColor else normalColor
                canvas.drawCircle(center.x, center.y, nodeRadius, nodePaint)
                if (isSelected) {
                    nodeDotPaint.color = activeColor
                    canvas.drawCircle(center.x, center.y, nodeRadius * 0.35f, nodeDotPaint)
                }
            }
        }

        companion object {
            private const val NODE_COUNT = 9
            private const val MIN_NODES = 4
            private const val ERROR_FLASH_DURATION_MS = 500L
        }
    }
