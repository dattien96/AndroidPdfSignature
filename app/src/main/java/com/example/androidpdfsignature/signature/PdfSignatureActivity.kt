package com.example.androidpdfsignature.signature

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidpdfsignature.R
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import kotlinx.android.synthetic.main.layout_signed_image.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min


class PdfSignatureActivity : AppCompatActivity() {

    private var signatureGestureHandler: SignatureGestureHandler? = null

    private var document: PDDocument? = null

    private val page: PDPage? by lazy {
        document?.getPage(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)

        if (document == null) document = PDDocument.load(assets.open("sample.pdf"))

        // Tính screen size cho move x,y của signature img
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        signatureGestureHandler =
            SignatureGestureHandler(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )

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
        }

        // Click save để lưu lại signature vào imageview
        layout_signature?.button_save_signature?.setOnClickListener {
            if (document == null) document = PDDocument.load(assets.open("sample.pdf"))

            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE
            button_attach_sign?.visibility = View.VISIBLE

            val originBitmap = layout_signature.signature_pad.transparentSignatureBitmap
            val cropTrimBitmap = CropTransparent().crop(originBitmap)
            layout_signed.image_signed?.setImageBitmap(cropTrimBitmap)
            layout_signed?.visibility = View.VISIBLE
        }

        // insert signature vào pdf
        button_attach_sign?.setOnClickListener {
            val drawable: BitmapDrawable =
                layout_signed.image_signed.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap
            val path = bitmapToFile(bitmap)
            fillForm(path ?: return@setOnClickListener)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSignatureDrag() {
        layout_signed?.apply {
            setOnTouchListener { view, event ->
                signatureGestureHandler?.handleTouchAction(view, event)
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
        pdf_view.fitToWidth()
    }

    private fun bitmapToFile(bitmap: Bitmap): String? {
        if (document == null || page == null) return null

        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "saved-signature.png")

        try {
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            var scaleBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * (page!!.bBox.width / pdf_view.optimalPageWidth)).toInt(),
                (bitmap.height * (page!!.bBox.height / pdf_view.optimalPageHeight)).toInt(),
                false
            )
            scaleBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    private fun fillForm(imagePath: String) {
        if (document == null || page == null) return
        try {
            var contentStream = PDPageContentStream(document, page, true, true)

            // Draw the red overlay image
            val insertImage = BitmapFactory.decodeFile(imagePath)
            val insertXImage = LosslessFactory.createFromImage(document, insertImage)
            val x = layout_signed.x * (page!!.bBox.width / pdf_view.optimalPageWidth)
            val y =
                (page!!.bBox.height - (layout_signed.y * (page!!.bBox.height / pdf_view.optimalPageHeight)))

            contentStream.drawImage(
                insertXImage,
                x,
                y - insertImage.height
            )
            contentStream.fill()
            // Make sure that the content stream is closed:
            contentStream.close()

            val paths: String = applicationContext.cacheDir.absolutePath + "/FilledForm.pdf"

            document!!.save(paths)
            document!!.close()
            document = null

            Toast.makeText(this, "Insert Done - check internal cache storage", Toast.LENGTH_SHORT)
                .show()
        } catch (ex: Exception) {
            val x = 1
        }
    }
}