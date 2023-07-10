package com.example.loginregister.imageprocessing.editor

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.example.loginregister.R
import kotlin.math.abs
import kotlin.math.atan2

@SuppressLint("ClickableViewAccessibility")
class DraggableTextView(context: Context) : AppCompatTextView(context) {

    private var mScaleGestureDetector: ScaleGestureDetector
    private var mRotationGestureDetector: RotationGestureDetector
    var draggableClickListener: DraggableClickListener? = null

    private var startX = 0f
    private var startY = 0f
    private var dX = 0f
    private var dY = 0f

    companion object {
        const val CLICK_ACTION_THRESHOLD = 5
    }

    interface DraggableClickListener {
        fun onClick(view: DraggableTextView)
    }

    init {
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        mRotationGestureDetector = RotationGestureDetector(RotationListener())

        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10f,
            context.resources.displayMetrics
        ).toInt()
        setPadding(padding, padding, padding, padding)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
            mScaleGestureDetector.onTouchEvent(event)
            mRotationGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = x - event.rawX
                    dY = y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val endX = event.rawX
                    val endY = event.rawY
                    if (abs(startX - endX) < CLICK_ACTION_THRESHOLD && abs(startY - endY) < CLICK_ACTION_THRESHOLD) {
                        draggableClickListener?.onClick(this)
                    }
                }

                else -> { /* Do nothing */ }
            }
        return true
    }

    class RotationGestureDetector
        (private val mListener: OnRotationGestureListener)
    {
        private var mPrevAngle: Float = 0.toFloat()

        interface OnRotationGestureListener {
            fun onRotation(rotationDetector: RotationGestureDetector)
        }

        var angle: Float = 0.toFloat()

        fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN ->
                    mPrevAngle = calculateAngle(event)

                MotionEvent.ACTION_MOVE ->
                    if (event.pointerCount > 1) {
                        angle = calculateAngle(event) - mPrevAngle
                        mListener.onRotation(this)
                    }
            }
            return true
        }

        private fun calculateAngle(event: MotionEvent): Float {
            val xTouch = event.getX(1) - event.getX(0)
            val yTouch = event.getY(1) - event.getY(0)
            return (atan2(yTouch.toDouble(), xTouch.toDouble())
                    * (180 / Math.PI)).toFloat()
        }
    }

    private inner class RotationListener : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(rotationDetector: RotationGestureDetector) {
            val angle = rotationDetector.angle
            rotation += angle
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newSize = textSize * scaleFactor
            setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize)
            return true
        }
    }

}
