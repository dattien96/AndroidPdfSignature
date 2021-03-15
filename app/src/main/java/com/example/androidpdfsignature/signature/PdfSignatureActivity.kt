package com.example.androidpdfsignature.signature

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.lifecycleScope
import com.example.androidpdfsignature.R
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.ProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class PdfSignatureActivity : AppCompatActivity() {

    companion object {
        private const val PDF_NAME = "sample5.pdf"
    }

    private var document: PDDocument? = null

    private var currentPage = 0
    private val displayMetrics = DisplayMetrics()
    private lateinit var cropTrimBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)

        if (document == null) document = PDDocument.load(assets.open(PDF_NAME))

        // Tính screen size cho spacing cua pdf
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // show pdf view
        showPdfFromAssets()

        // Click mở signature pad lấy chữ kí
        button_signature?.setOnClickListener {
            layout_signature?.visibility = View.VISIBLE
            button_signature?.visibility = View.GONE
            button_attach_sign?.visibility = View.GONE
        }

        // Click save để lưu lại signature vào imageview
        layout_signature?.button_save_signature?.setOnClickListener {
            if (document == null) document = PDDocument.load(assets.open(PDF_NAME))

            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE
            button_attach_sign?.visibility = View.VISIBLE

            val originBitmap = layout_signature.signature_pad.transparentSignatureBitmap
            cropTrimBitmap = CropTransparent().crop(originBitmap)

            renderPdfToImage()
        }

        // insert signature vào pdf
        button_attach_sign?.setOnClickListener {
            lifecycleScope.launch {
                showLoading()
                val path = bitmapToFile(cropTrimBitmap)
                fillForm(path ?: return@launch)
                showLoading(false)
                Toast.makeText(
                    this@PdfSignatureActivity,
                    "Insert Done - check internal cache storage",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    /**
     * Image của page pdf. Convert về image để có thể apply view zoom kính lúp
     * Phải đảm bảo hiển thị image này giống y hệt page pdf, cả về content và tọa độ
     */
    private fun renderPdfToImage() {
        lifecycleScope.launch {
            showLoading()
            val pageImage = withContext(Dispatchers.IO) {
                // Render the page and save it to an image file
                try {
                    // Load in an already created PDF
                    val document: PDDocument = PDDocument.load(assets.open(PDF_NAME))
                    // Create a renderer for the document
                    val renderer = PDFRenderer(document)
                    // Render the image to an RGB Bitmap
                    renderer.renderImage(currentPage, 1f, Bitmap.Config.RGB_565)
                } catch (e: IOException) {
                    Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e)
                    null
                }
            }
            image_choose_pdf?.setImageBitmap(pageImage)
            image_choose_pdf?.visibility = View.VISIBLE
            pdf_view?.visibility = View.INVISIBLE

            // Why doOnNextLayout() -> check doc
            image_choose_pdf?.doOnNextLayout {
                image_choose_pdf.setUpMagnifinerBitmap(
                    pageImage ?: return@doOnNextLayout,
                    cropTrimBitmap
                )
            }
            showLoading(false)
        }
    }

    private fun showPdfFromAssets() {
        pdf_view.fromAsset(PDF_NAME)
            .onPageChange { page, _ ->
                currentPage = page
            }
            .password(null) // if password protected, then write password
            .defaultPage(currentPage) // set the default page to open
            .onPageError { _, _ ->

            }
            .enableAnnotationRendering(true) // enable for filled form - color - comment
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

    private suspend fun bitmapToFile(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        delay(1000)
        if (document == null) {
            null
        } else {
            // Get the context wrapper
            val wrapper = ContextWrapper(applicationContext)

            // Initialize a new file instance to save bitmap object
            var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
            file = File(file, "saved-signature.png")

            val scaleByGesture = image_choose_pdf?.getSignatureImageScale() ?: 1f

            val page = document?.getPage(currentPage)
            if (page != null) {
                try {
                    // Compress the bitmap and save in jpg format
                    val stream: OutputStream = FileOutputStream(file)
                    val pageSize = pdf_view.getPageSize(currentPage)
                    var scaleBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * (page.bBox.width / pageSize.width) * scaleByGesture).toInt(),
                        (bitmap.height * (page.bBox.height / pageSize.height) * scaleByGesture).toInt(),
                        false
                    )
                    scaleBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                    stream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                file.absolutePath
            } else {
                null
            }
        }
    }

    private suspend fun fillForm(
        imagePath: String,
        isEncryptPdf: Boolean = false,
        isFormFieldReadOnly: Boolean = false
    ) =
        withContext(Dispatchers.IO) {
            if (document == null) {

            } else {
                val page = document?.getPage(currentPage)
                if (page != null) {
                    try {
                        var contentStream = PDPageContentStream(document, page, true, true)

                        // Draw the red overlay image
                        val pageSize = pdf_view.getPageSize(currentPage)

                        val insertImage = BitmapFactory.decodeFile(imagePath)
                        val insertXImage = LosslessFactory.createFromImage(document, insertImage)
                        val x =
                            image_choose_pdf.signatureBitmapX * (page.bBox.width / pageSize.width)
                        val y =
                            (page.bBox.height - (image_choose_pdf.signatureBitmapY * (page.bBox.height / pageSize.height)))

                        contentStream.drawImage(
                            insertXImage,
                            x,
                            y - insertImage.height
                        )
                        contentStream.fill()
                        // Make sure that the content stream is closed:
                        contentStream.close()

                        val paths: String =
                            applicationContext.cacheDir.absolutePath + "/FilledForm.pdf"

                        if (isFormFieldReadOnly) {
                            document!!.documentCatalog.acroForm?.fields?.let {
                                it.forEach { s ->
                                    s.isReadOnly = true
                                }
                            }
                        }

                        if (isEncryptPdf) {
                            document?.protect(
                                StandardProtectionPolicy(
                                    "ownerpass",
                                    "userpass",
                                    AccessPermission().apply {
                                        setCanModify(false)
                                        setCanFillInForm(false)
                                        setReadOnly()
                                        setCanModifyAnnotations(false)
                                        setCanExtractForAccessibility(false)
                                    })
                            )
                        }
                        document!!.save(paths)
                        document!!.close()
                        document = null
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

    private fun showLoading(show: Boolean = true) {
        view_loading.visibility = if (show) View.VISIBLE else View.GONE
    }
}
