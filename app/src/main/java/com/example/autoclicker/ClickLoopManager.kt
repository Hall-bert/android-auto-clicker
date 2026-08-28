package com.example.autoclicker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager

class ClickLoopManager {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var intervalMillis: Long = 5000

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val checkTask = object : Runnable {
        override fun run() {
            if (!isRunning) return
            eseguiCatturaEAnalisi()
            handler.postDelayed(this, intervalMillis)
        }
    }

    fun startLoop(context: Context, seconds: Int, resultCode: Int, data: Intent) {
        intervalMillis = seconds * 1000L
        if (isRunning) return

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        isRunning = true
        handler.post(checkTask)
    }

    fun stopLoop() {
        isRunning = false
        handler.removeCallbacks(checkTask)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun eseguiCatturaEAnalisi() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Controlla il pixel al centro esatto dello schermo
            val targetX = bitmap.width / 2
            val targetY = bitmap.height / 2
            val pixelColor = bitmap.getPixel(targetX, targetY)

            // Estrae i valori RGB del pixel
            val red = (pixelColor shr 16) and 0xFF
            val green = (pixelColor shr 8) and 0xFF
            val blue = pixelColor and 0xFF

            // Se il colore rilevato è prevalentemente rosso (R > 200, G < 80, B < 80)
            if (red > 200 && green < 80 && blue < 80) {
                ClickService.instance?.clickAt(targetX.toFloat(), targetY.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}
