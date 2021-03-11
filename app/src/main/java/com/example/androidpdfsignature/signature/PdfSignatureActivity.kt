package com.example.androidpdfsignature.signature

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.androidpdfsignature.R
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class PdfSignatureActivity : AppCompatActivity() {

    private var signatureGestureHandler: SignatureGestureHandler? = null

    private var document: PDDocument? = null

    private var page: PDPage? = null
        get() = document?.getPage(currentPage)

    private var currentPage = 0
    private val displayMetrics = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)

        if (document == null) document = PDDocument.load(assets.open("sample3.pdf"))

        // Tính screen size cho move x,y của signature img
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        signatureGestureHandler =
            SignatureGestureHandler(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )

        // show pdf view
        showPdfFromAssets("sample3.pdf")

        // setup drag signature imageview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupSignatureDrag()
        }

        // Click mở signature pad lấy chữ kí
        button_signature?.setOnClickListener {
            layout_signature?.visibility = View.VISIBLE
            button_signature?.visibility = View.GONE
            button_attach_sign?.visibility = View.GONE
            image_signed?.visibility = View.GONE
        }

        // Click save để lưu lại signature vào imageview
        layout_signature?.button_save_signature?.setOnClickListener {
            if (document == null) document = PDDocument.load(assets.open("sample3.pdf"))

            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE
            button_attach_sign?.visibility = View.VISIBLE

            val originBitmap = layout_signature.signature_pad.transparentSignatureBitmap
            val cropTrimBitmap = CropTransparent().crop(originBitmap)
            image_signed?.setImageBitmap(cropTrimBitmap)
            image_signed?.visibility = View.VISIBLE
        }

        // insert signature vào pdf
        button_attach_sign?.setOnClickListener {
            val drawable: BitmapDrawable =
                image_signed.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap
            val path = bitmapToFile(bitmap)
            fillForm(path ?: return@setOnClickListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSignatureDrag() {
        image_signed?.apply {
            setOnTouchListener { view, event ->
                signatureGestureHandler?.handleTouchAction(view, event)
                true
            }
        }
    }

    private fun showPdfFromAssets(pdfName: String) {
        pdf_view.fromAsset(pdfName)
            .onPageChange { page, _ ->
                currentPage = page
            }
            .password(null) // if password protected, then write password
            .defaultPage(0) // set the default page to open
            .onPageError { _, _ ->

            }
            .enableSwipe(false)
                // k đc dùng cái này mà phải define cụ thể spacing() -> xem doc
//            .autoSpacing(true)
                // spacing chỉ bật ở màn single page pdf sau khi user chọn để sign pdf
                // màn đọc pdf phải bỏ đi
            .spacing(displayMetrics.heightPixels)
            .load()
    }

    private fun bitmapToFile(bitmap: Bitmap): String? {
        if (document == null || page == null) return null

        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "saved-signature.png")

        val scaleByGesture = signatureGestureHandler?.getImageScale() ?: 1f

        try {
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            val pageSize = pdf_view.getPageSize(currentPage)
            var scaleBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * (page!!.bBox.width / pageSize.width) * scaleByGesture).toInt(),
                (bitmap.height * (page!!.bBox.height / pageSize.height) * scaleByGesture).toInt(),
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
            val pageSize = pdf_view.getPageSize(currentPage)

            val insertImage = BitmapFactory.decodeFile(imagePath)
            val insertXImage = LosslessFactory.createFromImage(document, insertImage)
            val x = image_signed.x * (page!!.bBox.width / pageSize.width)
            val y =
                (page!!.bBox.height - (image_signed.y * (page!!.bBox.height / pageSize.height)))

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