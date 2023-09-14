package com.obrekht.statsview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.withStyledAttributes
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

private const val START_ANGLE = -90F

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)

    private val textBounds = Rect()

    private var lineWidth = 5.dp(context).toFloat()
    private var fontSize = 40.dp(context).toFloat()
    private var colors = emptyList<Int>()
    private var progressBackgroundColor: Int = context.getColor(R.color.progress_background)

    var animationMode: AnimationMode = AnimationMode.Parallel

    private var progress = 0F
    private var valueAnimator: ValueAnimator? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            progressBackgroundColor = getColor(
                R.styleable.StatsView_android_progressBackgroundTint,
                progressBackgroundColor
            )
            animationMode = AnimationMode.values()[getInteger(
                R.styleable.StatsView_animationType,
                AnimationMode.Parallel.ordinal
            )]

            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            if (resId > 0) {
                colors = resources.getIntArray(resId).toList()
            }

        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = fontSize
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth / 2
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }

        val dataSum = data.sumOf { abs(it).toDouble() }
        var startFrom = START_ANGLE
        var overlapIndex = -1
        var overlapAngle = startFrom

        paint.color = progressBackgroundColor
        canvas.drawCircle(center.x, center.y, radius, paint)

        data.forEachIndexed { index, datum ->
            val angle = 360F * abs(datum) / dataSum.toFloat()

            if (datum > 0F) {
                if (overlapIndex < 0) {
                    overlapIndex = index
                    overlapAngle = startFrom
                }
                paint.color = colors.getOrNull(index) ?: getRandomColor()
                when (animationMode) {
                    AnimationMode.Parallel -> {
                        canvas.drawArc(oval, startFrom, angle * progress, false, paint)
                    }
                    AnimationMode.ParallelRotation -> {
                        canvas.drawArc(oval, startFrom + 360F * progress, angle * progress, false, paint)
                    }
                    AnimationMode.Sequential -> {
                        val currentAngle = START_ANGLE + progress * 360F
                        if (currentAngle >= startFrom) {
                            canvas.drawArc(oval, startFrom, angle * min(1F, (currentAngle - startFrom) / angle), false, paint)
                        }
                    }
                }
            }
            startFrom += angle
        }

        if (overlapIndex >= 0) {
            paint.color = colors.getOrNull(overlapIndex) ?: getRandomColor()
            when (animationMode) {
                AnimationMode.ParallelRotation -> {
                    canvas.drawArc(oval, overlapAngle + 360F * progress, 1F, false, paint)
                }
                else -> canvas.drawArc(oval, overlapAngle, 1F, false, paint)
            }
        }

        val text = "%.2f%%".format(progress * (data.filter { it > 0 }.sum() / dataSum * 100))
        textPaint.getTextBounds(
            text,
            0,
            text.length,
            textBounds
        )
        canvas.drawText(
            text,
            center.x,
            center.y + textBounds.height() / 2,
            textPaint
        )
    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = 3000
            interpolator = AccelerateDecelerateInterpolator()
        }.also {
            it.start()
        }
    }

    private fun getRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

    enum class AnimationMode {
        Parallel,
        ParallelRotation,
        Sequential
    }
}