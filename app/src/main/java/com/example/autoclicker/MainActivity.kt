package com.example.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity() {

    private val REQUEST_CODE_SCREEN_CAPTURE = 1001
    private lateinit var projectionManager: MediaProjectionManager
    private val loopManager = ClickLoopManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val editSeconds = findViewById<EditText>(R.id.editSeconds)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        }

        btnStop.setOnClickListener {
            loopManager.stopLoop()
            Toast.makeText(this, "Controllo fermato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val editSeconds = findViewById<EditText>(R.id.editSeconds)
            val seconds = editSeconds.text.toString().toIntOrNull() ?: 5
            
            loopManager.startLoop(this, seconds, resultCode, data)
            Toast.makeText(this, "Controllo avviato ogni $seconds secondi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permesso di cattura negato", Toast.LENGTH_SHORT).show()
        }
    }
}
