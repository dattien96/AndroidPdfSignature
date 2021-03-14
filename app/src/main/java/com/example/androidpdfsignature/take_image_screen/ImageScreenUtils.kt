package com.example.androidpdfsignature.take_image_screen

import android.app.Activity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

import android.view.View.MeasureSpec

fun takeScreenshotForScreen(activity: Activity): Bitmap? {
    return screenShotView(activity.window.decorView.rootView)
}

fun screenShotView(view: View, width: Int? = null, height: Int? = null): Bitmap? {
    val bitmap = Bitmap.createBitmap(
        width ?: view.width,
        height ?: view.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}