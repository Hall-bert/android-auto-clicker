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
        hideTarget()

        // Utilizziamo l'Application Context globale per s
