package com.example.androidpdfsignature.signature

import android.animation.ObjectAnimator
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import java.text.DecimalFormat
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min


class SignatureGestureHandler constructor(
    private val rootWidth: Int,
    private val rootHeight: Int
) {

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_ZOOM = 2
    }

    private var currentGestureMode = MODE_NONE
    private var mXDelta = 0
    private var mYDelta = 0

    // var for zoom image signature
    private var start = PointF()
    private var mid = PointF()
    private var oldDist = 1f
    private var d = 0f
    private var newRot = 0f
    private var lastEvent: FloatArray? = FloatArray(4)
    private val savedMatrix = Matrix()
    private val matrix = Matrix()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun handleTouchAction(view: View, event: MotionEvent) {
        val xScreenTouch = event.rawX.toInt() // x location relative to the screen
        val yScreenTouch = event.rawY.toInt() // y location relative to the screen

//        val df = DecimalFormat("##.####")
//        var originW = java.lang.Double.valueOf(df.format(view.width))
//        var originH = java.lang.Double.valueOf(df.format(view.height))
//
//        val originImageRatio = originW / originH
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                currentGestureMode = MODE_DRAG
                savedMatrix.set(matrix)
                start.set(event.getX(), event.getY())
                lastEvent = FloatArray(4)
                val lParams: RelativeLayout.LayoutParams =
                    view.layoutParams as RelativeLayout.LayoutParams
                mXDelta = xScreenTouch - lParams.leftMargin
                mYDelta = yScreenTouch - lParams.topMargin
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                savedMatrix.set(matrix)
                midPoint(mid, event)
                currentGestureMode = MODE_ZOOM

                lastEvent = FloatArray(4)
                lastEvent?.set(0, event.getX(0))
                lastEvent?.set(1, event.getX(1))
                lastEvent?.set(2, event.getY(0))
                lastEvent?.set(3, event.getY(1))
                d = rotation(event)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                currentGestureMode = MODE_DRAG
                lastEvent = null
            }
            MotionEvent.ACTION_UP -> {
                start = PointF()
                currentGestureMode = MODE_NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentGestureMode == MODE_DRAG) {
                    val layoutParams: RelativeLayout.LayoutParams = view
                        .layoutParams as RelativeLayout.LayoutParams
                    layoutParams.leftMargin = max(
                        0,
                        min(rootWidth - view.width, xScreenTouch - mXDelta)
                    )
                    layoutParams.topMargin = max(
                        0,
                        min(
                            rootHeight - view.height,
                            yScreenTouch - mYDelta
                        )
                    )
                    view.layoutParams = layoutParams
                } else if (currentGestureMode == MODE_ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
//                        matrix.postScale(scale, scale, mid.x, mid.y)
                        matrix.postScale(scale, scale)
                    }

                    if (lastEvent != null && event.pointerCount == 3) {
                        newRot = rotation(event)
                        val r = newRot - d
                        val values = FloatArray(9)
                        matrix.getValues(values)
                        val tx = values[2]
                        val ty = values[5]
                        val sx = values[0]
                        val xc = view.width / 2 * sx
                        val yc = view.height / 2 * sx
                        matrix.postRotate(r, tx + xc, ty + yc)
                    }
                }
            }
        }
        (view as ImageView).animationMatrix = matrix
    }

    fun getImageScale(): Float {
        val f = FloatArray(9)
        matrix.getValues(f)

        // scalex = scaleY
        return f[Matrix.MSCALE_X]
    }

    /**
     * Determine the space between the first two fingers
     */
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    /**
     * Calculate the degree to be rotated by.
     *
     * @param event
     * @return Degrees
     */
    private fun rotation(event: MotionEvent): Float {
        val delta_x = (event.getX(0) - event.getX(1)).toDouble()
        val delta_y = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(delta_y, delta_x)
        return Math.toDegrees(radians).toFloat()
    }
}