package com.example.dobalejemultimedia

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botón para ir a la carpeta de vídeos (aquí solo mostramos un Toast; implementa lo que necesites)
        val btnFolder: ImageButton = findViewById(R.id.btnFolder)
        btnFolder.setOnClickListener {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            startActivity(intent)
        }

        // Configuración de las tarjetas de vídeo
        val cardVideo1: CardView = findViewById(R.id.cardVideo1)
        val cardVideo2: CardView = findViewById(R.id.cardVideo2)
        val cardVideo3: CardView = findViewById(R.id.cardVideo3)

        // Suponemos que tienes vídeos en raw: video1.mp4, video2.mp4, video3.mp4
        cardVideo1.setOnClickListener { openRecordingActivity(R.raw.video1) }
        cardVideo2.setOnClickListener { openRecordingActivity(R.raw.video2) }
        cardVideo3.setOnClickListener { openRecordingActivity(R.raw.video3) }
    }

    private fun openRecordingActivity(videoResId: Int) {
        val intent = Intent(this, RecordingActivity::class.java)
        intent.putExtra("VIDEO_RES_ID", videoResId)
        startActivity(intent)
    }
}
