package com.example.androidpdfsignature

import android.app.Application
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
