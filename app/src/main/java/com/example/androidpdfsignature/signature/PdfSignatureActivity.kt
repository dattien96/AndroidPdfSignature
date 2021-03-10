package com.example.androidpdfsignature.signature

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import com.example.androidpdfsignature.R
import com.github.gcacace.signaturepad.views.SignaturePad
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import kotlinx.android.synthetic.main.layout_signed_image.*
import kotlinx.android.synthetic.main.layout_signed_image.view.*
import java.io.*
import kotlin.math.max
import kotlin.math.min


class PdfSignatureActivity : AppCompatActivity() {

    private var mXDelta = 0
    private var mYDelta = 0
    private var mRootWidth = 0
    private var mRootHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)

        // Tính screen size cho move x,y của signature img
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        mRootHeight = displayMetrics.heightPixels
        mRootWidth = displayMetrics.widthPixels


        // show pdf view
        showPdfFromAssets("sample.pdf")

        // setup drag signature imageview
        setupSignatureDrag()

        // Click mở signature pad lấy chữ kí
        button_signature?.setOnClickListener {
            layout_signature?.visibility = View.VISIBLE
            button_signature?.visibility = View.GONE
            button_attach_sign?.visibility = View.GONE
            layout_signed?.visibility = View.GONE

//            var arr1 = IntArray(2)
//            var arr2 = IntArray(2)
//            viewxx.getLocationOnScreen(arr1)
//            viewxx.getLocationInWindow(arr2)
//            Log.d("zxczxc", "screenH = $mRootHeight  - topPad = ${arr1[0]} - ${arr1[1]}  -- bottom = ${arr2[0]} - ${arr2[1]}")
//            Log.d("zxczxc", "screenH = $mRootHeight  - topPad = ${viewxx.height}")
        }

        // CLick save để lưu lại signature vào imageview
        layout_signature?.button_save_signature?.setOnClickListener {
            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE
            button_attach_sign?.visibility = View.VISIBLE

            layout_signed.image_signed?.setImageBitmap(layout_signature.signature_pad.transparentSignatureBitmap)
            layout_signed?.visibility = View.VISIBLE
        }

        // insert signature vào pdf
        button_attach_sign?.setOnClickListener {
            val drawable: BitmapDrawable =
                layout_signed.image_signed.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap
            val path = bitmapToFile(bitmap)
            fillForm(path)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSignatureDrag() {
        layout_signed?.apply {
            setOnTouchListener { view, event ->
                val xScreenTouch = event.rawX.toInt() // x location relative to the screen
                val yScreenTouch = event.rawY.toInt() // y location relative to the screen

                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        val lParams: RelativeLayout.LayoutParams =
                            view.layoutParams as RelativeLayout.LayoutParams
                        mXDelta = xScreenTouch - lParams.leftMargin
                        mYDelta = yScreenTouch - lParams.topMargin
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val layoutParams: RelativeLayout.LayoutParams = view
                            .layoutParams as RelativeLayout.LayoutParams
                        layoutParams.leftMargin = max(
                            0,
                            min(mRootWidth - view.width, xScreenTouch - mXDelta)
                        )
                        layoutParams.topMargin = max(
                            0,
                            min(
                                mRootHeight - view.height,
                                yScreenTouch - mYDelta
                            )
                        )
                        view.layoutParams = layoutParams
                    }
                }

                true
            }
        }
    }

    private fun showPdfFromAssets(pdfName: String) {
        pdf_view.fromAsset(pdfName)
            .password(null) // if password protected, then write password
            .defaultPage(0) // set the default page to open
            .onPageError { page, _ ->

            }
            .load()
    }

    private fun bitmapToFile(bitmap: Bitmap): String {
        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "saved-signature.png")

        try {
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            var scaleBitmap = Bitmap.createScaledBitmap(bitmap, 200, 100, false)
            scaleBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    private fun fillForm(imagePath: String) {
        try {
            val document: PDDocument = PDDocument.load(assets.open("sample.pdf"))
            val page: PDPage = document.getPage(0)

            var contentStream = PDPageContentStream(document, page, true, true)
//            contentStream.drawImage(image,
//                iv_sticker.getX() * (currentPage.getMediaBox().getWidth() / pdfView.getOptimalPageWidth()),
//                (currentPage.getMediaBox().getWidth() - (iv_sticker.getY() * (currentPage.getMediaBox().getHeight() / pdfView.getOptimalPageHeight()))) - (height),
//                width, height);
            // Draw the red overlay image
            val alphaImage = BitmapFactory.decodeFile(imagePath)
            val alphaXimage = LosslessFactory.createFromImage(document, alphaImage)
//            val x_image = x_pdf * width_image / width_page
//            val y_image = (height_pdf - y_pdf) * height_image / height_pdf
            val x = image_signed.x * (page.bBox.width / pdf_view.optimalPageWidth)
            val y = (page.bBox.width - (image_signed.y * (page.bBox.height / pdf_view.optimalPageHeight)))
            contentStream.drawImage(alphaXimage, x, y - alphaImage.height)
            Log.d("zxczxc", "${page.bBox.height}")

            contentStream.fill()
            // Make sure that the content stream is closed:
            contentStream.close()

            //
            val paths: String = applicationContext.cacheDir.absolutePath + "/FilledForm.pdf"

            document.save(paths)
            document.close()

            Toast.makeText(this, "Insert Done - check internal cache storage", Toast.LENGTH_SHORT)
                .show()
        } catch (ex: Exception) {

        }
    }
}