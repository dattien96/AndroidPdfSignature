package com.example.androidpdfsignature

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.androidpdfsignature.signature.PdfSignatureActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_demo1?.setOnClickListener {
            startActivity(Intent(this, DemoInsertImageActivity::class.java))
        }
        button_demo2?.setOnClickListener {
            startActivity(Intent(this, PdfSignatureActivity::class.java))
        }
    }
}