package com.example.autoclicker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class MediaCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "autoclicker_capture"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cattura Schermo",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setContentTitle("AutoClicker Colore")
            .setContentText("Cattura dello schermo attiva...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        // 1. Avviamo la notifica persistente obbligatoria per Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(101, notification)
        }

        // 2. Recuperiamo i dati e avviamo il loop in sicurezza dal contesto del servizio attivo
        if (intent != null) {
            val resultCode = intent.getIntExtra("RESULT_CODE", -1)
            val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("DATA_INTENT", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("DATA_INTENT")
            }
            val seconds = intent.getIntExtra("SECONDS", 5)
            val color = intent.getIntExtra("COLOR", 0xFFFF0000.toInt())

            if (resultCode != -1 && dataIntent != null) {
                ClickLoopManager.instance.startLoop(this, seconds, color, resultCode, dataIntent)
            }
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ClickLoopManager.instance.stopLoop()
    }
}
