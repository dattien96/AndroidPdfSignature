package com.example.androidpdfsignature.view_magnifier

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class ImageMagnifierView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_ZOOM = 2
    }

    private var currentGestureMode = MODE_NONE

    private var zoomPos: PointF? = PointF(0f, 0f)
    private var zooming = false
    private var shaderMatrix: Matrix? = Matrix()
    private var paint: Paint = Paint()
    private var bitmap: Bitmap? = null
    private val zoomShader: BitmapShader by lazy {
        BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    private val sizeOfMagnifier = 300
    private val targetLinePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    private var oldDist = 1f
    private lateinit var bitmapSignature: Bitmap
    private var signatureBitmapMatrix = Matrix()
    private var signatureBitmapPaint = Paint()
    private var isTouchOnSignature = false
    private var mid = PointF()

    private var touchMidZoomPercentX = 0f
    private var touchMidZoomPercentY = 0f

    // Khi touch vào bitmap thì sẽ độ chênh giữa điểm touch và top-left xy
    // tọa độ x y của view mình đang lưu là tính top left
    private var touchDifferX = 0f
    private var touchDifferY = 0f

    var signatureBitmapX = 0f
    var signatureBitmapY = 0f
    var isDrawSignature = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        zoomPos!!.x = event.x
        zoomPos!!.y = event.y
        when (action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {
                if (isTouchOnSignatureBitmap(event.x, event.y)) {
                    currentGestureMode = MODE_DRAG
                    isTouchOnSignature = true
                    touchDifferX = event.x - signatureBitmapX
                    touchDifferY = event.y - signatureBitmapY
                    this.invalidate()
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                midPoint(mid, event)
                currentGestureMode = MODE_ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTouchOnSignature) {
                    if (currentGestureMode == MODE_DRAG) {
                        val deltaX = event.x - signatureBitmapX - touchDifferX
                        val deltaY = event.y - signatureBitmapY - touchDifferY
                        signatureBitmapMatrix.postTranslate(deltaX, deltaY)
                        signatureBitmapX += deltaX
                        signatureBitmapY += deltaY
                        zooming = true
                        this.invalidate()
                    } else if (currentGestureMode == MODE_ZOOM) {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            val scale = newDist / oldDist

                            touchMidZoomPercentX =
                                (mid.x - signatureBitmapX) / (bitmapSignature.width * getSignatureImageScale())
                            touchMidZoomPercentY =
                                (mid.y - signatureBitmapY) / (bitmapSignature.height * getSignatureImageScale())

                            signatureBitmapMatrix.postScale(scale, scale, mid.x, mid.y)

                            // Update x,y by scale
                            signatureBitmapX =
                                mid.x - touchMidZoomPercentX * bitmapSignature.width * getSignatureImageScale()
                            signatureBitmapY =
                                mid.y - touchMidZoomPercentY * bitmapSignature.height * getSignatureImageScale()
                            oldDist = newDist
                        }
                        this.invalidate()
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                currentGestureMode = MODE_DRAG
                oldDist = 1f
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                zooming = false
                isTouchOnSignature = false
                touchDifferX = 0f
                touchDifferY = 0f
                mid = PointF()
                touchMidZoomPercentX = 0f
                touchMidZoomPercentY = 0f
                currentGestureMode = MODE_NONE
                this.invalidate()
            }
            else -> {
            }
        }
        return true
    }

    fun setUpMagnifinerBitmap(pdfBitmap: Bitmap, signatureBitmap: Bitmap) {
        bitmap = Bitmap.createScaledBitmap(pdfBitmap, width, height, false)
        bitmapSignature = signatureBitmap
        isDrawSignature = true
        invalidate()
    }

    fun getSignatureImageScale(): Float {
        val f = FloatArray(9)
        signatureBitmapMatrix.getValues(f)

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

    private fun isTouchOnSignatureBitmap(xTouch: Float, yTouch: Float): Boolean {
        if (xTouch >= signatureBitmapX && xTouch <= signatureBitmapX + bitmapSignature.width * getSignatureImageScale()
            && yTouch >= signatureBitmapY && yTouch <= signatureBitmapY + bitmapSignature.height * getSignatureImageScale()
        ) return true

        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (zooming) {
            if ((bitmapSignature.height * getSignatureImageScale()) <= (sizeOfMagnifier / 1.5) &&
                (bitmapSignature.width * getSignatureImageScale()) <= (sizeOfMagnifier / 1.5)
            ) {
                try {
                    paint = Paint().apply {
                        this.shader = zoomShader
                    }
                    shaderMatrix?.apply {
                        reset()
                        postScale(
                            2f,
                            2f,
                            signatureBitmapX,
                            signatureBitmapY
                                    + (bitmapSignature.height * getSignatureImageScale())
                        )
                    }
                    paint.shader.setLocalMatrix(shaderMatrix)

                    canvas.drawCircle(
                        signatureBitmapX,
                        signatureBitmapY + (bitmapSignature.height * getSignatureImageScale()),
                        sizeOfMagnifier.toFloat(),
                        paint
                    )

                    canvas.drawLine(
                        signatureBitmapX,
                        signatureBitmapY + (bitmapSignature.height * getSignatureImageScale()),
                        signatureBitmapX,
                        signatureBitmapY - sizeOfMagnifier.toFloat() + (bitmapSignature.height * getSignatureImageScale()),
                        targetLinePaint
                    )

                    canvas.drawLine(
                        signatureBitmapX,
                        signatureBitmapY + (bitmapSignature.height * getSignatureImageScale()),
                        signatureBitmapX + sizeOfMagnifier.toFloat(),
                        signatureBitmapY + (bitmapSignature.height * getSignatureImageScale()),
                        targetLinePaint
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        if (isDrawSignature) {

            // bật đoạn này để check background khi scale hay zoom
//            canvas.drawRect(
//                signatureBitmapX,
//                signatureBitmapY,
//                signatureBitmapX + bitmapSignature.width * getSignatureImageScale(),
//                signatureBitmapY + bitmapSignature.height * getSignatureImageScale(),
//                targetLinePaint
//            )
            canvas.drawBitmap(bitmapSignature, signatureBitmapMatrix, signatureBitmapPaint)
        }
    }
}