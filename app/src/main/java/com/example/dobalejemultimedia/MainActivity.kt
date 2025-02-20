package com.example.dobalejemultimedia

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

//Menu de selección de vídeo que el usuario quiere doblar además de la ruta donde se almacenan
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botón para ir a la carpeta de descargas donde almaceno los doblajes
        val btnFolder: ImageButton = findViewById(R.id.btnFolder)
        btnFolder.setOnClickListener {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            startActivity(intent)
        }

        // Configuración de las tarjetas de vídeo
        val cardVideo1: CardView = findViewById(R.id.cardVideo1)
        val cardVideo2: CardView = findViewById(R.id.cardVideo2)
        val cardVideo3: CardView = findViewById(R.id.cardVideo3)

        // Acción para mostrar los vídeos
        cardVideo1.setOnClickListener { openRecordingActivity(R.raw.video1) }
        cardVideo2.setOnClickListener { openRecordingActivity(R.raw.video2) }
        cardVideo3.setOnClickListener { openRecordingActivity(R.raw.video3) }
    }

    // Navegar para la actividad de doblaje
    private fun openRecordingActivity(videoResId: Int) {
        val intent = Intent(this, RecordingActivity::class.java)
        intent.putExtra("VIDEO_RES_ID", videoResId)
        startActivity(intent)
    }
}
