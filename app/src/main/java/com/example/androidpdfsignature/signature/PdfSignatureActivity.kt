package com.example.androidpdfsignature.signature

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.androidpdfsignature.R
import com.github.gcacace.signaturepad.views.SignaturePad
import kotlinx.android.synthetic.main.activity_pdf_signature.*
import kotlinx.android.synthetic.main.layout_insert_signature.view.*
import kotlinx.android.synthetic.main.layout_signed_image.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class PdfSignatureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_signature)
        showPdfFromAssets("sample.pdf")
        button_signature?.setOnClickListener {
            layout_signature?.visibility = View.VISIBLE
            button_signature?.visibility = View.GONE
            layout_signed?.visibility = View.GONE
        }

        layout_signature?.button_save_signature?.setOnClickListener {
            layout_signature?.visibility = View.GONE
            button_signature?.visibility = View.VISIBLE

            layout_signed.image_signed?.setImageBitmap(layout_signature.signature_pad.signatureBitmap)
//            layout_signature.image_signed?.setImageBitmap(layout_signature.signature_pad.getTransparentSignatureBitmap(true))
            layout_signed?.visibility = View.VISIBLE
            val savedPath = bitmapToFile(layout_signature.signature_pad.signatureBitmap)
            Toast.makeText(this, "Signature saved into: " + savedPath.encodedPath, Toast.LENGTH_LONG).show()
        }

        signaturePadSetup()
    }

    private fun signaturePadSetup() {
        layout_signature.signature_pad?.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {
                Log.d("asdasd", "start")
            }

            override fun onClear() {

            }

            override fun onSigned() {
                Log.d("asdasd", "sign")
            }

        })
    }

    private fun showPdfFromAssets(pdfName: String) {
        pdf_view.fromAsset(pdfName)
            .password(null) // if password protected, then write password
            .defaultPage(0) // set the default page to open
            .onPageError { page, _ ->

            }
            .load()
    }

    private fun bitmapToFile(bitmap: Bitmap): Uri {
        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file,"saved-signature.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        return Uri.parse(file.absolutePath)
    }
}