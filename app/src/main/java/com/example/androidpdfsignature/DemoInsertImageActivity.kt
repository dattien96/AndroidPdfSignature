package com.example.androidpdfsignature

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.android.synthetic.main.activity_demo_insert_image.*
import java.io.InputStream

class DemoInsertImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_insert_image)
        button_insert?.setOnClickListener {
            fillForm()
        }
    }

    private fun fillForm() {
        try {
            val document: PDDocument = PDDocument.load(assets.open("sample.pdf"))
            val page: PDPage = document.getPage(0)

            var contentStream = PDPageContentStream(document, page, true, true)

            // Load in the images

            // Load in the images
            val img: InputStream = assets.open("falcon.jpg")
            val alpha: InputStream = assets.open("trans.png")

            // Draw the falcon base image

            // Draw the falcon base image
            val ximage = JPEGFactory.createFromStream(document, img)
            contentStream.drawImage(ximage, 100f, 330f)

            // Draw the red overlay image

            // Draw the red overlay image
            val alphaImage = BitmapFactory.decodeStream(alpha)
            val alphaXimage = LosslessFactory.createFromImage(document, alphaImage)
            contentStream.drawImage(alphaXimage, 100f, 100f)
            contentStream.fill()
            // Make sure that the content stream is closed:
            contentStream.close()

            //
            val paths: String = applicationContext.cacheDir.absolutePath + "/FilledForm.pdf"

            document.save(paths)
            document.close()

            Toast.makeText(this, "Insert Done - check internal cache storage", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {

        }
    }
}