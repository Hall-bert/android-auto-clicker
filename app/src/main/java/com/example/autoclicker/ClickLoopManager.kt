package com.example.autoclicker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class ClickLoopManager {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var intervalMillis: Long = 5000
    private var targetColor: Int = Color.RED

    private var windowManager: WindowManager? = null
    private var targetView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var currentTargetX: Int = -1
    private var currentTargetY: Int = -1

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

    fun showTarget(context: Context) {
        // Chiude e pulisce l'eventuale mirino precedente rimasto nascosto o bloccato
        hideTarget()

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = context.resources.displayMetrics.density
        val viewSize = (40 * density).toInt()

        targetView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(100, 255, 0, 0))
                setStroke((3 * density).toInt(), Color.RED)
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            viewSize,
            viewSize,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            // Ripristina l'ultima coordinata nota se disponibile, altrimenti va al centro dello schermo
            x = if (currentTargetX != -1) currentTargetX - viewSize / 2 else context.resources.displayMetrics.widthPixels / 2 - viewSize / 2
            y = if (currentTargetY != -1) currentTargetY - viewSize / 2 else context.resources.displayMetrics.heightPixels / 2 - viewSize / 2
        }

        if (currentTargetX == -1 || currentTargetY == -1) {
            currentTargetX = params!!.x + viewSize / 2
            currentTargetY = params!!.y + viewSize / 2
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        targetView?.setOnTouchListener { v, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()

                    val metrics = context.resources.displayMetrics
                    if (p.x < 0) p.x = 0
                    if (p.y < 0) p.y = 0
                    if (p.x > metrics.widthPixels - viewSize) p.x = metrics.widthPixels - viewSize
                    if (p.y > metrics.heightPixels - viewSize) p.y = metrics.heightPixels - viewSize

                    windowManager?.updateViewLayout(targetView, p)

                    currentTargetX = p.x + viewSize / 2
                    currentTargetY = p.y + viewSize / 2
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(targetView, params)
    }

    fun hideTarget() {
        if (targetView != null) {
            try {
                windowManager?.removeView(targetView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            targetView = null
            windowManager = null
        }
    }

    fun startLoop(context: Context, seconds: Int, color: Int, resultCode: Int, data: Intent) {
        intervalMillis = seconds * 1000L
        targetColor = color
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

            val targetX = if (currentTargetX in 0 until bitmap.width) currentTargetX else bitmap.width / 2
            val targetY = if (currentTargetY in 0 until bitmap.height) currentTargetY else bitmap.height / 2

            val pixelColor = bitmap.getPixel(targetX, targetY)

            val redDetected = Color.red(pixelColor)
            val greenDetected = Color.green(pixelColor)
            val blueDetected = Color.blue(pixelColor)

            val redTarget = Color.red(targetColor)
            val greenTarget = Color.green(targetColor)
            val blueTarget = Color.blue(targetColor)

            val tolerance = 50
            val redDiff = Math.abs(redDetected - redTarget)
            val greenDiff = Math.abs(greenDetected - greenTarget)
            val blueDiff = Math.abs(blueDetected - blueTarget)

            if (redDiff < tolerance && greenDiff < tolerance && blueDiff < tolerance) {
                ClickService.instance?.clickAt(targetX.toFloat(), targetY.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}
