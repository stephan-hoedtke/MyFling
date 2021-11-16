package com.stho.myfling

import android.R.attr
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation


class FlingView : View {

    interface OnRotateListener {
        fun onRotate(delta: Double)
    }

    interface OnDoubleTapListener {
        fun onDoubleTap()
    }

    private var rotateListener: OnRotateListener? = null
    private var doubleTapListener: OnDoubleTapListener? = null
    private var previousAngle: Double = 0.0
    private var startPosition: Float = 0f
    private var previousValue: Float = 0f
    private var gestureDetector: GestureDetector? = null
    private var velocityTracker: VelocityTracker? = null
    private var angle: Double = 0.0
    private val path: Path = Path()
    private val paint: Paint = Paint().apply {
        this.color = Color.argb(255, 34, 102, 59)
        this.strokeWidth = 3f
        this.style = Paint.Style.FILL_AND_STROKE
    }

    private val fling = FlingAnimation(this, DynamicAnimation.Z).apply {
        addUpdateListener { animation, value, velocity -> onUpdateFling(value, velocity) }
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    private fun setup() {
        setupGestureDetector()
        setupVelocityTracker()
     }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                this@FlingView.onDown(e)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                this@FlingView.onDoubleTap()
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                this@FlingView.onFling(e1, e2, velocityX, velocityY)
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                this@FlingView.onScroll(e1, e2, distanceX, distanceY)
                return true
            }
        })
    }

    private fun setupVelocityTracker() {
        velocityTracker = VelocityTracker.obtain()
    }

    private fun onRotate(delta: Double) {
        if (rotateListener != null) rotateListener!!.onRotate(delta)
    }

    private fun onDoubleTap() {
        if (doubleTapListener != null) doubleTapListener!!.onDoubleTap()
    }

    private fun onDown(e: MotionEvent) {
        fling.cancel()
        previousAngle = getAngle(e.y)
    }

    private fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) {
        Log.d("GESTURE", "onScroll($distanceX, $distanceY)")
        val alpha = getAngle(e2.y)
        val delta = ensureAngleInRange(alpha - previousAngle)
        previousAngle = alpha
        onRotate(delta)
    }

    private fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float) {
        Log.d("GESTURE", "onFling($velocityX, $velocityY)")
        fling.apply {
            previousAngle = getAngle(e2.y)
            startPosition = e2.y
            previousValue = 0f
            setStartVelocity(velocityY)
            setStartValue(0f)
            friction = 1.1f
            start()
        }
    }

    private fun onUpdateFling(value: Float, velocity: Float) {
        Log.d("GESTURE", "onUpdateFling($value, $velocity)")
        val distance = value - previousValue
        val alpha = getAngle(startPosition + distance)
        val delta = ensureAngleInRange(alpha - previousAngle)
        previousValue = value
        onRotate(delta)
    }

    fun setOnRotateListener(listener: OnRotateListener?) {
        rotateListener = listener
    }

    fun setOnDoubleTapListener(listener: OnDoubleTapListener?) {
        doubleTapListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        velocityTracker?.addMovement(event)
        return true
    }

    private fun getAngle(y: Float): Double {
        val l = height / 2
        val d = l - y
        val r = d / l
        return -Degree.arcCos(r.coerceIn(-1f, +1f).toDouble())
    }

    internal fun setRotationAngle(newAngle: Double) {
        if (angle != newAngle) {
            angle = newAngle
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()
        val h = height.toDouble()
        val w = width.toDouble()
        val l = h / 2
        for (alpha in 10..360 step 20) {
            val beta = Degree.normalizeTo180(angle - alpha)
            if (-90.0 <= beta && beta <= 90.0) {
                val y = l * (1 - Degree.sin(beta))
                val x0 = 0f
                val x1 = w.toFloat()
                val y0 = y.toFloat()
                path.moveTo(x0, y0)
                path.lineTo(x1, y0)
            }
        }
        canvas.drawPath(path, paint)

        val defaultTextSize = resources.getDimensionPixelSize(R.dimen.myFontSize).toFloat()

        for (alpha in 0..350 step 20) {
            val beta = Degree.normalizeTo180(angle - alpha)
            if (-90.0 <= beta && beta <= 90.0) {
                val y = (l * (1 - Degree.sin(beta))).toFloat()
                val text = alpha.toString()
                paint.textSize = defaultTextSize
                val textWith = paint.measureText(text)
                val x0 = (w.toFloat() - textWith) / 2
                val y0 = y + defaultTextSize / 2
                canvas.save()
                canvas.scale(1f, Degree.cos(beta).toFloat(), 0f, y)
                canvas.drawText(alpha.toString(), x0, y0, paint)
                canvas.restore()
            }
        }
        canvas.drawPath(path, paint)
    }

    companion object {

        private fun ensureAngleInRange(delta: Double): Double {
            var x = delta
            while (x > 180) {
                x -= 360.0
            }
            while (x < -180) {
                x += 360.0
            }
            return x
        }
    }
}