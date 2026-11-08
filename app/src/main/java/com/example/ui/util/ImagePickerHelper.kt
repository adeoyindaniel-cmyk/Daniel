package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.InputStream

object ImagePickerHelper {
    private const val TAG = "ImagePickerHelper"

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize if exceeds 1024x1024 to keep payload well within 20MB
            if (originalBitmap != null) {
                val maxDim = 1024
                val width = originalBitmap.width
                val height = originalBitmap.height
                if (width > maxDim || height > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(width, height)
                    val newWidth = (width * scale).toInt()
                    val newHeight = (height * scale).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
        }
    }
}
