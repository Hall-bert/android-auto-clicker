package com.example.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity() {

    private val REQUEST_CODE_SCREEN_CAPTURE = 1001
    private val REQUEST_CODE_OVERLAY = 1002
    private lateinit var projectionManager: MediaProjectionManager
    private val loopManager = ClickLoopManager()
    private var isTargetVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val editSeconds = findViewById<EditText>(R.id.editSeconds)
        val btnToggleTarget = findViewById<Button>(R.id.btnToggleTarget)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnToggleTarget.setOnClickListener {
            if (isTargetVisible) {
                loopManager.hideTarget()
                btnToggleTarget.text = "Mostra Mirino Target"
                isTargetVisible = false
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Abilita il permesso di disegno sopra altre app", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                } else {
                    loopManager.showTarget(this)
                    btnToggleTarget.text = "Nascondi Mirino Target"
                    isTargetVisible = true
                }
            }
        }

        btnStart.setOnClickListener {
            // Forziamo il sistema ad avviare la cattura solo ed esclusivamente per l'intero display
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
            } else {
                projectionManager.createScreenCaptureIntent()
            }
            startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE)
        }

        btnStop.setOnClickListener {
            loopManager.stopLoop()
            Toast.makeText(this, "Controllo fermato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                loopManager.showTarget(this)
                val btnToggleTarget = findViewById<Button>(R.id.btnToggleTarget)
                btnToggleTarget.text = "Nascondi Mirino Target"
                isTargetVisible = true
            } else {
                Toast.makeText(this, "Permesso sovrapposizione negato", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val editSeconds = findViewById<EditText>(R.id.editSeconds)
            val seconds = editSeconds.text.toString().toIntOrNull() ?: 5

            val editColor = findViewById<EditText>(R.id.editColor)
            val colorHex = editColor.text.toString().trim()
            val targetColor = try {
                val cleanHex = if (colorHex.startsWith("#")) colorHex.substring(1) else colorHex
                val formattedHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
                java.lang.Long.parseLong(formattedHex, 16).toInt()
            } catch (e: Exception) {
                0xFFFF0000.toInt()
            }

            loopManager.startLoop(this, seconds, targetColor, resultCode, data)
            Toast.makeText(this, "Controllo avviato ogni $seconds secondi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loopManager.hideTarget()
    }
}
