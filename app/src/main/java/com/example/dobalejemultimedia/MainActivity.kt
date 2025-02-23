package com.example.dobalejemultimedia

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import java.io.File

// Menú de selección de vídeo que el usuario quiere doblar y opciones adicionales.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botón para abrir la carpeta de descargas (donde se almacenan los doblajes)
        val btnFolder: ImageButton = findViewById(R.id.btnFolder)
        btnFolder.setOnClickListener {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            startActivity(intent)
        }

        // Botón para grabar un vídeo con título personalizado.
        // Al pulsarlo se muestra un diálogo para que el usuario introduzca el título,
        // y a continuación se lanza un chooser para que elija la app de cámara.
        val btnRecordVideo: ImageButton = findViewById(R.id.btnRecordVideo)
        btnRecordVideo.setOnClickListener {
            val editText = EditText(this)
            val dialog = AlertDialog.Builder(this)
                .setTitle("Introduce el título para el vídeo")
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val title = editText.text.toString().trim()
                    if (title.isNotEmpty()) {
                        // Ruta de salida en la carpeta de Descargas usando el título ingresado
                        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val outputFile = File(downloadsFolder, "$title.mp4")

                        // Usar FileProvider para obtener una URI con esquema "content://"
                        val contentUri = FileProvider.getUriForFile(
                            this,
                            "${applicationContext.packageName}.fileprovider",
                            outputFile
                        )


                        // Intent para capturar vídeo
                        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                        // Conceder permisos temporales a la app de cámara para acceder a la URI
                        videoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        val chooser = Intent.createChooser(videoIntent, "Elige una aplicación para grabar vídeo")
                        startActivity(chooser)
                    } else {
                        Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }


        // Configuración de las tarjetas de vídeo.
        val cardVideo1: CardView = findViewById(R.id.cardVideo1)
        val cardVideo2: CardView = findViewById(R.id.cardVideo2)
        val cardVideo3: CardView = findViewById(R.id.cardVideo3)

        // Acción para mostrar los vídeos (inicia la actividad de doblaje).
        cardVideo1.setOnClickListener { openRecordingActivity(R.raw.video1) }
        cardVideo2.setOnClickListener { openRecordingActivity(R.raw.video2) }
        cardVideo3.setOnClickListener { openRecordingActivity(R.raw.video3) }
    }

    // Navega a la actividad de doblaje, pasando el recurso del vídeo seleccionado.
    private fun openRecordingActivity(videoResId: Int) {
        val intent = Intent(this, RecordingActivity::class.java)
        intent.putExtra("VIDEO_RES_ID", videoResId)
        startActivity(intent)
    }
}
