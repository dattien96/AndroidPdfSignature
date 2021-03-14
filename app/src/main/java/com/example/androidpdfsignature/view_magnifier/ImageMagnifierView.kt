package com.example.androidpdfsignature.view_magnifier

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.example.androidpdfsignature.R
import com.example.androidpdfsignature.signature.IMagnifierInteraction
import java.lang.Exception


class ImageMagnifierView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var zoomPos: PointF? = PointF(0f, 0f)
    private var zooming = false
    private var shaderMatrix: Matrix? = Matrix()
    private var paint: Paint = Paint()
    private var bitmap: Bitmap? = null
    private val zoomShader: BitmapShader by lazy {
        BitmapShader(bitmap!!, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }
    private val sizeOfMagnifier = 300
    private var iMagnifierInteraction: IMagnifierInteraction? = null
    private val targetLinePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        zoomPos!!.x = event.x
        zoomPos!!.y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                zooming = true
                this.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                zooming = false
                this.invalidate()
            }
            else -> {
            }
        }
        return true
    }

    fun setMagnifierInteraction(i: IMagnifierInteraction) {
        iMagnifierInteraction = i
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!zooming) {
            buildDrawingCache()
        } else {
            if ((iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()) > (sizeOfMagnifier / 1.5)) return
            if ((iMagnifierInteraction!!.getSignatureView().width * iMagnifierInteraction!!.getBitmapScale()) > (sizeOfMagnifier / 1.5)) return

            try {
                bitmap = drawingCache
                paint = Paint().apply {
                    this.shader = zoomShader
                }
                shaderMatrix?.apply {
                    reset()
                    postScale(
                        2f,
                        2f,
                        iMagnifierInteraction!!.getSignatureView().x,
                        iMagnifierInteraction!!.getSignatureView().y
                                + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale())
                    )
                }
                paint.shader.setLocalMatrix(shaderMatrix)

                canvas.drawCircle(
                    iMagnifierInteraction!!.getSignatureView().x,
                    iMagnifierInteraction!!.getSignatureView().y + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()),
                    sizeOfMagnifier.toFloat(),
                    paint
                )

                canvas.drawLine(
                    iMagnifierInteraction!!.getSignatureView().x,
                    iMagnifierInteraction!!.getSignatureView().y + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()),
                    iMagnifierInteraction!!.getSignatureView().x,
                    iMagnifierInteraction!!.getSignatureView().y - sizeOfMagnifier.toFloat() + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()),
                    targetLinePaint
                )

                canvas.drawLine(
                    iMagnifierInteraction!!.getSignatureView().x,
                    iMagnifierInteraction!!.getSignatureView().y + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()),
                    iMagnifierInteraction!!.getSignatureView().x + sizeOfMagnifier.toFloat(),
                    iMagnifierInteraction!!.getSignatureView().y + (iMagnifierInteraction!!.getSignatureView().height * iMagnifierInteraction!!.getBitmapScale()),
                    targetLinePaint
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}