package com.example.androidpdfsignature.signature

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidpdfsignature.R
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class PdfSignatureActivity : AppCompatActivity(), IMagnifierInteraction {

    companion object {
        private const val PDF_NAME = "sample5.pdf"
    }

    private var signatureGestureHandler: SignatureGestureHandler? = null

    private var document: PDDocument? = null

    private var page: PDPage? = null
        get() = document?.getPage(currentPage)

    private var currentPage = 0
    private val displayMetrics = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)

        if (document == null) document = PDDocument.load(assets.open(PDF_NAME))

        // Tính screen size cho move x,y của signature img
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        signatureGestureHandler =
            SignatureGestureHandler(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                image_choose_pdf
            )

        // show pdf view
        showPdfFromAssets(PDF_NAME)

        // setup drag signature imageview
        setupSignatureDrag()

        // Click mở signature pad lấy chữ kí
        button_signature?.setOnClickListener {
            layout_signature?.visibility = View.VISIBLE
            button_signature?.visibility = View.GONE
            button_attach_sign?.visibility = View.GONE
            image_signed?.visibility = View.GONE
        }

        // Click save để lưu lại signature vào imageview
        layout_signature?.button_save_signature?.setOnClickListener {
            if (document == null) document = PDDocument.load(assets.open(PDF_NAME))

            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE
            button_attach_sign?.visibility = View.VISIBLE

            val originBitmap = layout_signature.signature_pad.transparentSignatureBitmap
            val cropTrimBitmap = CropTransparent().crop(originBitmap)
            image_signed?.setImageBitmap(cropTrimBitmap)
            image_signed?.visibility = View.VISIBLE

            renderPdfToImage()
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

    /**
     * Image của page pdf. Convert về image để có thể apply view zoom kính lúp
     * Phải đảm bảo hiển thị image này giống y hệt page pdf, cả về content và tọa độ
     */
    private fun renderPdfToImage() {
        // Render the page and save it to an image file
        try {
            // Load in an already created PDF
            val document: PDDocument = PDDocument.load(assets.open(PDF_NAME))
            // Create a renderer for the document
            val renderer = PDFRenderer(document)
            // Render the image to an RGB Bitmap
           val  pageImage = renderer.renderImage(currentPage, 1f, Bitmap.Config.ARGB_8888)
            image_choose_pdf?.setImageBitmap(pageImage)
            image_choose_pdf?.visibility = View.VISIBLE
            pdf_view?.visibility = View.INVISIBLE
            image_choose_pdf.setMagnifierInteraction(this)
        } catch (e: IOException) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e)
        }
    }

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
            .defaultPage(currentPage) // set the default page to open
            .onPageError { _, _ ->

            }
            .enableSwipe(false)
                // k đc dùng cái này mà phải define cụ thể spacing() -> xem doc
//            .autoSpacing(true)
                // spacing chỉ bật ở màn single page pdf sau khi user chọn để sign pdf
                // màn đọc pdf phải bỏ đi
            .spacing(displayMetrics.heightPixels)
            .enableDoubletap(false) // tắt zoom bằng click
            .load()

        // tắt zoom swipe
        // Case này k cần tương tác j nên set như này ok, vì n tắt cả scroll
        pdf_view.setOnTouchListener(null)
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
            ex.printStackTrace()
        }
    }

    override fun getSignatureView(): ImageView {
        return image_signed
    }

    override fun getBitmapScale(): Float {
        return signatureGestureHandler?.getImageScale() ?: 1f
    }
}

interface IMagnifierInteraction {
    fun getSignatureView(): ImageView
    fun getBitmapScale(): Float
}